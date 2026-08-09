package org.traccar.game.speedhunt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.GameMapUpdateService;
import org.traccar.game.notification.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.GameSpeedhunt;
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

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class GameSpeedhuntService {

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
    private GameNotificationService notificationService;

    @Inject
    private GameMapUpdateService mapUpdateService;

    public GameSpeedhunt startSpeedhunt(
            long userId, long gameId, long targetMemberId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanStartSpeedhunt(userId, gameId);
        if (context == null) {
            return null;
        }

        Game game = context.game();
        if (game.getSpeedhuntLimit() <= 0) {
            throw new IllegalArgumentException("Speedhunts are disabled");
        }

        List<GameSpeedhunt> speedhunts = getSpeedhunts(gameId);
        if (speedhunts.size() >= game.getSpeedhuntLimit()) {
            throw new IllegalArgumentException("No speedhunts remaining");
        }
        if (getActiveSpeedhunt(speedhunts) != null) {
            throw new IllegalArgumentException("A speedhunt is already active");
        }

        GameMember target = getTargetMember(gameId, targetMemberId);
        if (target == null) {
            throw new IllegalArgumentException("Speedhunt target not found");
        }
        validateTarget(target);

        GameSpeedhunt previous = speedhunts.isEmpty() ? null : speedhunts.get(speedhunts.size() - 1);
        if (previous != null
                && !game.getAllowConsecutiveSpeedhuntsSameTarget()
                && previous.getTargetMemberId() == targetMemberId) {
            throw new IllegalArgumentException("Consecutive speedhunts on the same target are not allowed");
        }

        GameSpeedhunt speedhunt = new GameSpeedhunt();
        speedhunt.setGameId(gameId);
        speedhunt.setSequenceNumber(speedhunts.size() + 1);
        speedhunt.setTargetMemberId(targetMemberId);
        speedhunt.setCreatedByUserId(userId);
        speedhunt.setMaxPings(game.getSpeedhuntPingLimit());
        speedhunt.setStartedAt(new Date());
        speedhunt.setId(storage.addObject(speedhunt, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, speedhunt);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_SPEEDHUNT_STARTED);
        message.setSpeedhuntId(speedhunt.getId());
        notificationService.notifyGameMembers(gameId, message);

        createSpeedhuntPing(context, speedhunt, request);

        return speedhunt;
    }

    public GamePing requestSpeedhuntPing(
            long userId, long gameId, long speedhuntId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanRequestSpeedhuntPing(userId, gameId);
        if (context == null) {
            return null;
        }

        GameSpeedhunt speedhunt = getSpeedhunt(gameId, speedhuntId);
        if (speedhunt == null) {
            return null;
        }
        return createSpeedhuntPing(context, speedhunt, request);
    }

    public GameSpeedhunt finishSpeedhunt(
            long userId, long gameId, long speedhuntId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireGameManagement(userId, gameId);
        if (context == null) {
            return null;
        }

        GameSpeedhunt speedhunt = getSpeedhunt(gameId, speedhuntId);
        if (speedhunt == null) {
            return null;
        }
        finishSpeedhunt(context, speedhunt, request);
        return speedhunt;
    }

    private GamePing createSpeedhuntPing(
            GameRuntimeContext context, GameSpeedhunt speedhunt, HttpServletRequest request) throws Exception {
        if (speedhunt.getEndedAt() != null) {
            throw new IllegalArgumentException("Speedhunt is not active");
        }

        GameMember target = getTargetMember(context.game().getId(), speedhunt.getTargetMemberId());
        if (target == null) {
            throw new IllegalArgumentException("Speedhunt target not found");
        }
        validateTarget(target);

        List<GamePing> pings = getSpeedhuntPings(context.game().getId(), speedhunt.getId());
        if (pings.size() >= speedhunt.getMaxPings()) {
            throw new IllegalArgumentException("Speedhunt ping limit reached");
        }

        GamePendingEffect effect = getNextPendingEffect(context.game().getId(), target.getId());
        GamePing ping = new GamePing();
        ping.setGameId(context.game().getId());
        ping.setTargetMemberId(target.getId());
        ping.setSpeedhuntId(speedhunt.getId());
        ping.setSequenceNumber(pings.size() + 1);
        ping.setCreatedAt(new Date());

        if (effect != null && GamePendingEffect.EFFECT_SKIP_NEXT_PING.equals(effect.getEffect())) {
            ping.setSource(GamePing.SOURCE_SPEEDHUNT);
            ping.setSkipped(true);
            ping.setConsumedJokerId(effect.getJokerId());
        } else if (effect != null && GamePendingEffect.EFFECT_FAKE_NEXT_PING.equals(effect.getEffect())) {
            applyFakePing(ping, effect);
        } else {
            applyRealPosition(context.game(), target, ping);
        }

        ping.setId(storage.addObject(ping, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, context.userId(), ping);

        if (effect != null) {
            consumeEffect(context.userId(), effect, ping, request);
        }

        mapUpdateService.notifySpeedhuntPingCreated(ping);

        if (ping.getSequenceNumber() >= speedhunt.getMaxPings()) {
            finishSpeedhunt(context, speedhunt, request);
        }

        return ping;
    }

    private void applyRealPosition(Game game, GameMember target, GamePing ping) throws StorageException {
        Player player = getPlayer(target.getPlayerId());
        if (player == null || player.getDeviceId() == 0) {
            throw new IllegalArgumentException("Speedhunt target has no valid device");
        }

        Position position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(player.getDeviceId())));
        if (position == null) {
            throw new IllegalArgumentException("No position found for speedhunt target");
        }
        if (isPositionTooOld(game, position)) {
            throw new IllegalArgumentException("Latest target position is too old");
        }

        ping.setSource(GamePing.SOURCE_SPEEDHUNT);
        ping.setPositionId(position.getId());
        ping.setFixTime(position.getFixTime());
        ping.setLatitude(position.getLatitude());
        ping.setLongitude(position.getLongitude());
        ping.setAccuracy(position.getAccuracy());
    }

    private boolean isPositionTooOld(Game game, Position position) {
        return game.getMaxPositionAgeSeconds() > 0
                && (position.getFixTime() == null
                        || position.getFixTime().before(
                                new Date(System.currentTimeMillis() - game.getMaxPositionAgeSeconds() * 1000L)));
    }

    private void applyFakePing(GamePing ping, GamePendingEffect effect) {
        try {
            JsonNode payload = objectMapper.readTree(effect.getPayload());
            if (!payload.has("latitude") || !payload.has("longitude")) {
                throw new IllegalArgumentException("Fake ping payload requires latitude and longitude");
            }
            ping.setSource(GamePing.SOURCE_FAKE);
            ping.setLatitude(payload.get("latitude").asDouble());
            ping.setLongitude(payload.get("longitude").asDouble());
            if (payload.has("accuracy")) {
                ping.setAccuracy(payload.get("accuracy").asDouble());
            }
            ping.setFixTime(new Date());
            ping.setConsumedJokerId(effect.getJokerId());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid fake ping payload", e);
        }
    }

    private void consumeEffect(
            long userId, GamePendingEffect effect, GamePing ping, HttpServletRequest request) throws Exception {
        GamePendingEffect update = new GamePendingEffect();
        update.setId(effect.getId());
        update.setActive(false);
        update.setConsumedAt(new Date());
        update.setConsumedPingId(ping.getId());
        storage.updateObject(update, new Request(
                new Columns.Include("active", "consumedAt", "consumedPingId"),
                new Condition.Equals("id", effect.getId())));
        cacheManager.invalidateObject(true, GamePendingEffect.class, effect.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        if (effect.getJokerId() != 0) {
            GameJoker joker = new GameJoker();
            joker.setId(effect.getJokerId());
            joker.setStatus(GameJoker.STATUS_USED);
            joker.setUsedAt(new Date());
            storage.updateObject(joker, new Request(
                    new Columns.Include("status", "usedAt"),
                    new Condition.Equals("id", effect.getJokerId())));
            cacheManager.invalidateObject(true, GameJoker.class, effect.getJokerId(), ObjectOperation.UPDATE);
            actionLogger.edit(request, userId, joker);
        }
    }

    private void finishSpeedhunt(
            GameRuntimeContext context, GameSpeedhunt speedhunt, HttpServletRequest request) throws Exception {
        if (speedhunt.getEndedAt() != null) {
            return;
        }

        Date endedAt = new Date();
        GameSpeedhunt update = new GameSpeedhunt();
        update.setId(speedhunt.getId());
        update.setEndedAt(endedAt);
        storage.updateObject(update, new Request(
                new Columns.Include("endedAt"),
                new Condition.Equals("id", speedhunt.getId())));
        cacheManager.invalidateObject(true, GameSpeedhunt.class, speedhunt.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, context.userId(), update);
        speedhunt.setEndedAt(endedAt);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                context.game().getId(), GameNotificationMessage.TYPE_SPEEDHUNT_FINISHED);
        message.setSpeedhuntId(speedhunt.getId());
        notificationService.notifyGameMembers(context.game().getId(), message);
    }

    private void validateTarget(GameMember target) {
        if (!GameMember.ROLE_HUNTED.equals(target.getRole())) {
            throw new IllegalArgumentException("Speedhunt target must be a hunted player");
        }
        if (!GameMember.STATUS_ACTIVE.equals(target.getStatus())) {
            throw new IllegalArgumentException("Speedhunt target must be active");
        }
    }

    private GameMember getTargetMember(long gameId, long targetMemberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", targetMemberId),
                        new Condition.Equals("gameId", gameId))));
    }

    private Player getPlayer(long playerId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
    }

    private List<GameSpeedhunt> getSpeedhunts(long gameId) throws StorageException {
        return storage.getObjects(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("sequenceNumber")));
    }

    private GameSpeedhunt getSpeedhunt(long gameId, long speedhuntId) throws StorageException {
        return storage.getObject(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", speedhuntId),
                        new Condition.Equals("gameId", gameId))));
    }

    private GameSpeedhunt getActiveSpeedhunt(List<GameSpeedhunt> speedhunts) {
        for (GameSpeedhunt speedhunt : speedhunts) {
            if (speedhunt.getEndedAt() == null) {
                return speedhunt;
            }
        }
        return null;
    }

    private List<GamePing> getSpeedhuntPings(long gameId, long speedhuntId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("speedhuntId", speedhuntId)), new Order("sequenceNumber")));
    }

    private GamePendingEffect getNextPendingEffect(long gameId, long memberId) throws StorageException {
        List<GamePendingEffect> effects = storage.getObjects(GamePendingEffect.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("memberId", memberId)),
                        new Condition.Equals("active", true)), new Order("id")));

        GamePendingEffect fake = null;
        for (GamePendingEffect effect : effects) {
            if (GamePendingEffect.EFFECT_SKIP_NEXT_PING.equals(effect.getEffect())) {
                return effect;
            }
            if (fake == null && GamePendingEffect.EFFECT_FAKE_NEXT_PING.equals(effect.getEffect())) {
                fake = effect;
            }
        }
        return fake;
    }

}
