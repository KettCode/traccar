package org.traccar.game.map;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.message.GameMapUpdateMessage;
import org.traccar.game.map.view.GameMapGeofence;
import org.traccar.game.map.view.GameMapMarker;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.session.GameConnectionManager;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.Player;
import org.traccar.storage.StorageException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class GameMapUpdateService {

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameConnectionManager gameConnectionManager;

    @Inject
    private GameNotificationService notificationService;

    @Inject
    private GameMapMapper mapMapper;

    public void notifySpeedhuntPingCreated(GamePing ping) throws StorageException {
        notifyPingsCreated(List.of(ping), GameNotificationMessage.TYPE_SPEEDHUNT_PING_CREATED);
    }

    public void notifyRegularPingCreated(GamePing ping) throws StorageException {
        notifyRegularPingsCreated(List.of(ping));
    }

    public void notifyRegularPingsCreated(List<GamePing> pings) throws StorageException {
        notifyPingsCreated(pings, GameNotificationMessage.TYPE_REGULAR_PING_CREATED);
    }

    private void notifyPingsCreated(List<GamePing> pings, String notificationType) throws StorageException {
        if (pings.isEmpty()) {
            return;
        }

        long gameId = pings.get(0).getGameId();
        List<GameMember> members = gameStorage.getGameMembers(gameId);
        Map<Long, GameMember> membersById = indexMembers(members);

        var markers = new ArrayList<GameMapMarker>();
        for (GamePing ping : pings) {
            GameMember target = membersById.get(ping.getTargetMemberId());
            if (target != null) {
                GameMapMarker marker = mapMapper.toPingMarker(ping, target);
                if (marker != null) {
                    markers.add(marker);
                }
            }
        }

        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        if (markers.isEmpty()) {
            notifyActiveHunters(members, playersById, createPingStateChangedMessage(gameId, notificationType, pings));
            return;
        }

        for (long userId : getRecipientUserIds(members, playersById, runtimePermissionService::canReceivePingMapUpdates)) {
            GameMapUpdateMessage update = createUpdate(gameId, GameMapUpdateMessage.TYPE_GAME_POSITION_UPDATED, false);
            update.getMarkers().addAll(markers);
            gameConnectionManager.updateGameMap(userId, update);
        }
    }

    private GameNotificationMessage createPingStateChangedMessage(
            long gameId, String notificationType, List<GamePing> pings) {
        GameNotificationMessage message = notificationService.createStateChangedMessage(gameId, notificationType);
        if (GameNotificationMessage.TYPE_SPEEDHUNT_PING_CREATED.equals(notificationType) && pings.size() == 1) {
            GamePing ping = pings.get(0);
            message.setSpeedhuntId(ping.getSpeedhuntId());
            message.setPingId(ping.getId());
        }
        return message;
    }

    private void notifyActiveHunters(
            List<GameMember> members, Map<Long, Player> playersById, GameNotificationMessage message) {
        for (long userId : getRecipientUserIds(
                members, playersById, runtimePermissionService::canReceivePingMapUpdates)) {
            gameConnectionManager.updateGameNotification(userId, message);
        }
    }

    public void notifyGeofenceUpdated(GameGeofence gameGeofence) throws StorageException {
        Game game = gameStorage.getGame(gameGeofence.getGameId());
        if (game == null) {
            return;
        }

        if (!gameGeofence.getActive()) {
            notifyGeofenceRemoved(gameGeofence.getGameId(), gameGeofence.getId());
            return;
        }

        GameMapGeofence geofence = toGeofence(gameGeofence);
        if (geofence == null) {
            return;
        }

        List<GameMember> members = gameStorage.getGameMembers(gameGeofence.getGameId());
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : members) {
            if (!runtimePermissionService.canReceiveMapUpdates(member)) {
                continue;
            }
            Player player = playersById.get(member.getPlayerId());
            if (player == null || player.getUserId() == 0 || !notifiedUserIds.add(player.getUserId())) {
                continue;
            }

            GameRuntimeContext context = new GameRuntimeContext(player.getUserId(), game, member, player);
            if (runtimePermissionService.canViewGeofence(context, gameGeofence)) {
                GameMapUpdateMessage update = createUpdate(
                        gameGeofence.getGameId(), GameMapUpdateMessage.TYPE_GAME_GEOFENCE_UPDATED, false);
                update.getGeofences().add(geofence);
                gameConnectionManager.updateGameMap(player.getUserId(), update);
            }
        }
    }

    public void notifyGeofenceRemoved(long gameId, long gameGeofenceId) throws StorageException {
        GameMapUpdateMessage update = createUpdate(
                gameId, GameMapUpdateMessage.TYPE_GAME_GEOFENCE_UPDATED, true);
        update.getRemovedGeofenceIds().add(gameGeofenceId);
        notifyGameMembers(gameId, update);
    }

    private void notifyGameMembers(long gameId, GameMapUpdateMessage update) throws StorageException {
        List<GameMember> members = gameStorage.getGameMembers(gameId);
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        for (long userId : getRecipientUserIds(
                members, playersById, runtimePermissionService::canReceiveMapUpdates)) {
            gameConnectionManager.updateGameMap(userId, update);
        }
    }

    private Set<Long> getRecipientUserIds(
            List<GameMember> members, Map<Long, Player> playersById, Predicate<GameMember> filter) {
        Set<Long> userIds = new HashSet<>();
        for (GameMember member : members) {
            if (!filter.test(member)) {
                continue;
            }
            long playerUserId = getUserId(member, playersById);
            if (playerUserId != 0) {
                userIds.add(playerUserId);
            }
        }
        return userIds;
    }

    private long getUserId(GameMember member, Map<Long, Player> playersById) {
        Player player = playersById.get(member.getPlayerId());
        return player != null ? player.getUserId() : 0;
    }

    private GameMapUpdateMessage createUpdate(long gameId, String type, boolean stateRefresh) {
        GameMapUpdateMessage update = new GameMapUpdateMessage();
        update.setGameId(gameId);
        update.setType(type);
        update.setStateRefresh(stateRefresh);
        return update;
    }

    private GameMapGeofence toGeofence(GameGeofence gameGeofence) throws StorageException {
        return mapMapper.toGeofence(gameGeofence, gameStorage.getGeofence(gameGeofence.getGeofenceId()));
    }

    private Map<Long, GameMember> indexMembers(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            result.put(member.getId(), member);
        }
        return result;
    }

}
