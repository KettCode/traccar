package org.traccar.game.joker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.joker.request.ActivateJokerRequest;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.GamePushNotificationService;
import org.traccar.helper.LogAction;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GameReveal;
import org.traccar.model.GameRevealedPosition;
import org.traccar.model.GameSpeedhunt;
import org.traccar.model.Geofence;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameJokerService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameNotificationService notificationService;

    @Inject
    private GamePushNotificationService pushNotificationService;

    public GameJoker unlockJoker(
            long userId, long gameId, long memberId, String type, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanUnlockJoker(userId, gameId);
        if (context == null) {
            return null;
        }

        validateJokerType(type);
        GameMember member = gameStorage.getGameMember(gameId, memberId);
        if (member == null) {
            throw new IllegalArgumentException("Joker member not found");
        }
        validateJokerOwner(member);

        GameJoker joker = new GameJoker();
        joker.setGameId(gameId);
        joker.setMemberId(memberId);
        joker.setType(type);
        joker.setStatus(GameJoker.STATUS_UNLOCKED);
        joker.setUnlockedAt(new Date());
        joker.setUnlockedByUserId(userId);
        joker.setId(storage.addObject(joker, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, joker);
        notifyJokerChanged(gameId, joker);
        pushNotificationService.notifyJokerUnlocked(gameId, joker);
        return joker;
    }

    public GameJoker activateJoker(
            long userId, long gameId, long jokerId, ActivateJokerRequest entity,
            HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanUseJoker(userId, gameId, jokerId);
        if (context == null) {
            return null;
        }

        GameJoker joker = gameStorage.getGameJoker(gameId, jokerId);
        if (joker == null) {
            return null;
        }
        if (!GameJoker.STATUS_UNLOCKED.equals(joker.getStatus())) {
            throw new IllegalArgumentException("Joker is not unlocked");
        }

        GameMember owner = gameStorage.getGameMember(gameId, joker.getMemberId());
        if (owner == null) {
            throw new IllegalArgumentException("Joker member not found");
        }
        validateJokerOwner(owner);

        switch (joker.getType()) {
            case GameJoker.TYPE_SKIP_PING -> {
                createPendingEffect(context, joker, GamePendingEffect.EFFECT_SKIP_NEXT_PING, null, request);
                markJokerActivated(context.userId(), joker.getId(), request);
            }
            case GameJoker.TYPE_FAKE_PING -> {
                createPendingEffect(context, joker, GamePendingEffect.EFFECT_FAKE_NEXT_PING, getFakePayload(context.game().getId(), entity), request);
                markJokerActivated(context.userId(), joker.getId(), request);
            }
            case GameJoker.TYPE_REVEAL_SPEEDHUNT -> {
                createSpeedhuntReveal(context, joker, request);
                markJokerUsed(context.userId(), joker.getId(), request);
            }
            case GameJoker.TYPE_REQUEST_HUNTER_LOCATIONS -> {
                createHunterLocationReveal(context, joker, request);
                markJokerUsed(context.userId(), joker.getId(), request);
            }
            default -> throw new IllegalArgumentException("Invalid joker type");
        }

        Player ownerPlayer = gameStorage.getPlayer(owner.getPlayerId());
        if (ownerPlayer != null && ownerPlayer.getUserId() != userId) {
            notifyJokerChanged(gameId, joker);
            pushNotificationService.notifyJokerActivated(gameId, joker);
        }

        return gameStorage.getGameJoker(gameId, jokerId);
    }

    public GameJoker cancelJoker(long userId, long gameId, long jokerId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireGameManagement(userId, gameId);
        if (context == null) {
            return null;
        }

        GameJoker joker = gameStorage.getGameJoker(gameId, jokerId);
        if (joker == null) {
            return null;
        }
        if (GameJoker.STATUS_USED.equals(joker.getStatus())) {
            throw new IllegalArgumentException("Used Jokers cannot be cancelled");
        }

        cancelPendingEffects(userId, gameId, jokerId, request);

        GameJoker update = new GameJoker();
        update.setId(jokerId);
        update.setStatus(GameJoker.STATUS_CANCELLED);
        update.setCancelledAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "cancelledAt"),
                new Condition.Equals("id", jokerId)));
        cacheManager.invalidateObject(true, GameJoker.class, jokerId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        joker.setStatus(GameJoker.STATUS_CANCELLED);
        joker.setCancelledAt(update.getCancelledAt());
        notifyJokerChanged(gameId, joker);
        return joker;
    }

    private void createPendingEffect(
            GameRuntimeContext context, GameJoker joker, String effect, String payload,
            HttpServletRequest request) throws Exception {
        GamePendingEffect pendingEffect = new GamePendingEffect();
        pendingEffect.setGameId(context.game().getId());
        pendingEffect.setMemberId(joker.getMemberId());
        pendingEffect.setJokerId(joker.getId());
        pendingEffect.setEffect(effect);
        pendingEffect.setActive(true);
        pendingEffect.setPayload(payload);
        pendingEffect.setCreatedAt(new Date());
        pendingEffect.setId(storage.addObject(pendingEffect, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, context.userId(), pendingEffect);
    }

    private void createSpeedhuntReveal(GameRuntimeContext context, GameJoker joker, HttpServletRequest request) throws Exception {
        GameSpeedhunt speedhunt = gameStorage.getActiveGameSpeedhunt(context.game().getId());
        if (speedhunt == null) {
            throw new IllegalArgumentException("No active speedhunt found");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("targetMemberId", speedhunt.getTargetMemberId());

        createReveal(context.userId(), context.game().getId(), joker, GameReveal.TYPE_SPEEDHUNT_TARGET,
                speedhunt.getId(), payload.toString(), request);
    }

    private void createHunterLocationReveal(GameRuntimeContext context, GameJoker joker, HttpServletRequest request) throws Exception {
        GameReveal reveal = createReveal(context.userId(), context.game().getId(), joker, GameReveal.TYPE_HUNTER_LOCATIONS,0, null, request);
        List<GameMember> hunters = gameStorage.getActiveHunterMembers(context.game().getId());
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(hunters);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(
                playersById.values().stream()
                        .map(Player::getDeviceId)
                        .filter(deviceId -> deviceId != 0)
                        .collect(Collectors.toSet()));
        for (GameMember member : hunters) {
            Player player = playersById.get(member.getPlayerId());
            if (player == null || player.getDeviceId() == 0) {
                continue;
            }
            Position position = latestPositionsByDeviceId.get(player.getDeviceId());
            if (position == null) {
                continue;
            }

            GameRevealedPosition revealedPosition = new GameRevealedPosition();
            revealedPosition.setRevealId(reveal.getId());
            revealedPosition.setMemberId(member.getId());
            revealedPosition.setPositionId(position.getId());
            revealedPosition.setFixTime(position.getFixTime());
            revealedPosition.setLatitude(position.getLatitude());
            revealedPosition.setLongitude(position.getLongitude());
            revealedPosition.setAccuracy(position.getAccuracy());
            revealedPosition.setId(storage.addObject(revealedPosition, new Request(new Columns.Exclude("id"))));
            actionLogger.create(request, context.userId(), revealedPosition);
        }
    }

    private GameReveal createReveal(
            long userId, long gameId, GameJoker joker, String type, long speedhuntId, String payload,
            HttpServletRequest request) throws StorageException {
        GameReveal reveal = new GameReveal();
        reveal.setGameId(gameId);
        reveal.setMemberId(joker.getMemberId());
        reveal.setJokerId(joker.getId());
        reveal.setType(type);
        reveal.setSpeedhuntId(speedhuntId);
        reveal.setPayload(payload);
        reveal.setRevealedAt(new Date());
        reveal.setId(storage.addObject(reveal, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, reveal);
        return reveal;
    }

    private String getFakePayload(long gameId, ActivateJokerRequest entity) throws StorageException {
        if (entity == null || entity.getLatitude() == null || entity.getLongitude() == null) {
            throw new IllegalArgumentException("Fake ping requires latitude and longitude");
        }
        validateCoordinates(entity.getLatitude(), entity.getLongitude());
        validateFakePingInActivePlayfield(gameId, entity.getLatitude(), entity.getLongitude());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("latitude", entity.getLatitude());
        payload.put("longitude", entity.getLongitude());
        if (entity.getAccuracy() != null) {
            payload.put("accuracy", entity.getAccuracy());
        }
        return payload.toString();
    }

    private void validateFakePingInActivePlayfield(long gameId, double latitude, double longitude) throws StorageException {
        List<GameGeofence> playfields = gameStorage.getActiveGameGeofencesByType(gameId, GameGeofence.TYPE_PLAYFIELD);
        if (playfields.isEmpty()) {
            return;
        }

        Map<Long, Geofence> geofencesById = gameStorage.getGeofencesByGameGeofences(playfields);
        for (GameGeofence playfield : playfields) {
            Geofence geofence = geofencesById.get(playfield.getGeofenceId());
            if (geofence != null && geofence.getGeometry().containsPoint(latitude, longitude)) {
                return;
            }
        }
        throw new IllegalArgumentException("Fake ping must be inside the active playfield");
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude out of range");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude out of range");
        }
    }

    private void notifyJokerChanged(long gameId, GameJoker joker) throws StorageException {
        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_JOKER_CHANGED);
        message.setJokerId(joker.getId());
        notificationService.notifyMember(gameId, joker.getMemberId(), message);
    }

    private void markJokerActivated(long userId, long jokerId, HttpServletRequest request) throws Exception {
        GameJoker update = new GameJoker();
        update.setId(jokerId);
        update.setStatus(GameJoker.STATUS_ACTIVATED);
        update.setActivatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "activatedAt"),
                new Condition.Equals("id", jokerId)));
        cacheManager.invalidateObject(true, GameJoker.class, jokerId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);
    }

    private void markJokerUsed(long userId, long jokerId, HttpServletRequest request) throws Exception {
        GameJoker update = new GameJoker();
        update.setId(jokerId);
        update.setStatus(GameJoker.STATUS_USED);
        update.setUsedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "usedAt"),
                new Condition.Equals("id", jokerId)));
        cacheManager.invalidateObject(true, GameJoker.class, jokerId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);
    }

    private void cancelPendingEffects(
            long userId, long gameId, long jokerId, HttpServletRequest request) throws Exception {
        var effects = storage.getObjects(GamePendingEffect.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("jokerId", jokerId)),
                        new Condition.Equals("active", true)), new Order("id")));
        for (GamePendingEffect effect : effects) {
            GamePendingEffect update = new GamePendingEffect();
            update.setId(effect.getId());
            update.setActive(false);
            update.setConsumedAt(new Date());
            storage.updateObject(update, new Request(
                    new Columns.Include("active", "consumedAt"),
                    new Condition.Equals("id", effect.getId())));
            cacheManager.invalidateObject(true, GamePendingEffect.class, effect.getId(), ObjectOperation.UPDATE);
            actionLogger.edit(request, userId, update);
        }
    }

    private void validateJokerOwner(GameMember member) {
        if (!GameMember.ROLE_HUNTED.equals(member.getRole())) {
            throw new IllegalArgumentException("Joker owner must be a hunted player");
        }
        if (!GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            throw new IllegalArgumentException("Joker owner must be active");
        }
    }

    private void validateJokerType(String type) {
        if (!GameJoker.TYPE_SKIP_PING.equals(type)
                && !GameJoker.TYPE_FAKE_PING.equals(type)
                && !GameJoker.TYPE_REVEAL_SPEEDHUNT.equals(type)
                && !GameJoker.TYPE_REQUEST_HUNTER_LOCATIONS.equals(type)) {
            throw new IllegalArgumentException("Invalid joker type");
        }
    }

}
