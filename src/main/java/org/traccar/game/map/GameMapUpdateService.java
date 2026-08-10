package org.traccar.game.map;

import jakarta.inject.Inject;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.GameService;
import org.traccar.game.notification.GameConnectionManager;
import org.traccar.game.notification.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameMapUpdateService {

    @Inject
    private Storage storage;

    @Inject
    private GameService gameService;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameConnectionManager gameConnectionManager;

    @Inject
    private GameNotificationService notificationService;

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
        List<GameMember> members = getMembers(gameId);
        Map<Long, GameMember> membersById = new HashMap<>();
        for (GameMember member : members) {
            membersById.put(member.getId(), member);
        }

        var markers = new ArrayList<GameMapMarker>();
        for (GamePing ping : pings) {
            GameMember target = membersById.get(ping.getTargetMemberId());
            if (target != null) {
                GameMapMarker marker = toMarker(ping, target);
                if (marker != null) {
                    markers.add(marker);
                }
            }
        }

        if (markers.isEmpty()) {
            GameNotificationMessage message = notificationService.createStateChangedMessage(
                    gameId, notificationType);
            if (pings.size() == 1) {
                GamePing ping = pings.get(0);
                message.setSpeedhuntId(ping.getSpeedhuntId());
                message.setPingId(ping.getId());
            }
            notifyActiveHunters(gameId, message);
            return;
        }

        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : members) {
            if (!canReceivePingUpdate(member)) {
                continue;
            }
            Player player = getPlayer(member.getPlayerId());
            if (player == null || player.getUserId() == 0 || !notifiedUserIds.add(player.getUserId())) {
                continue;
            }

            GameMapUpdateMessage update = createUpdate(
                    gameId, GameMapUpdateMessage.TYPE_GAME_POSITION_UPDATED, true);
            update.getMarkers().addAll(markers);
            gameConnectionManager.updateGameMap(player.getUserId(), update);
        }
    }

    private void notifyActiveHunters(long gameId, GameNotificationMessage message) throws StorageException {
        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : getMembers(gameId)) {
            if (!canReceivePingUpdate(member)) {
                continue;
            }
            Player player = getPlayer(member.getPlayerId());
            if (player != null && player.getUserId() != 0 && notifiedUserIds.add(player.getUserId())) {
                gameConnectionManager.updateGameNotification(player.getUserId(), message);
            }
        }
    }

    public void notifyGeofenceUpdated(GameGeofence gameGeofence) throws StorageException {
        Game game = gameService.getGame(gameGeofence.getGameId());
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

        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : getMembers(gameGeofence.getGameId())) {
            if (!canReceiveMapUpdate(member)) {
                continue;
            }
            Player player = getPlayer(member.getPlayerId());
            if (player == null || player.getUserId() == 0 || !notifiedUserIds.add(player.getUserId())) {
                continue;
            }

            GameRuntimeContext context = new GameRuntimeContext(player.getUserId(), game, member, player);
            if (runtimePermissionService.canViewGeofence(context, gameGeofence)) {
                GameMapUpdateMessage update = createUpdate(
                        gameGeofence.getGameId(), GameMapUpdateMessage.TYPE_GAME_GEOFENCE_UPDATED, true);
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
        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : getMembers(gameId)) {
            if (!canReceiveMapUpdate(member)) {
                continue;
            }
            Player player = getPlayer(member.getPlayerId());
            if (player != null && player.getUserId() != 0 && notifiedUserIds.add(player.getUserId())) {
                gameConnectionManager.updateGameMap(player.getUserId(), update);
            }
        }
    }

    private GameMapUpdateMessage createUpdate(long gameId, String type, boolean stateRefresh) {
        GameMapUpdateMessage update = new GameMapUpdateMessage();
        update.setGameId(gameId);
        update.setType(type);
        update.setStateRefresh(stateRefresh);
        return update;
    }

    private GameMapMarker toMarker(GamePing ping, GameMember target) {
        if (ping.getSkipped()) {
            return null;
        }

        GameMapMarker marker = new GameMapMarker();
        marker.setGameId(ping.getGameId());
        marker.setMemberId(target.getId());
        marker.setDisplayName(target.getDisplayName());
        marker.setRole(target.getRole());
        marker.setStatus(target.getStatus());
        marker.setSource(getClientSource(ping));
        marker.setPingId(ping.getId());
        if (ping.getSpeedhuntId() != 0) {
            marker.setSpeedhuntId(ping.getSpeedhuntId());
        }
        if (ping.getPositionId() != 0) {
            marker.setPositionId(ping.getPositionId());
        }
        marker.setFixTime(ping.getFixTime());
        marker.setLatitude(ping.getLatitude());
        marker.setLongitude(ping.getLongitude());
        marker.setAccuracy(ping.getAccuracy());
        return marker;
    }

    private String getClientSource(GamePing ping) {
        if (ping.getSpeedhuntId() != 0) {
            return GamePing.SOURCE_SPEEDHUNT;
        }
        return GamePing.SOURCE_REGULAR;
    }

    private GameMapGeofence toGeofence(GameGeofence gameGeofence) throws StorageException {
        Geofence geofence = storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameGeofence.getGeofenceId())));
        if (geofence == null) {
            return null;
        }

        GameMapGeofence view = new GameMapGeofence();
        view.setId(gameGeofence.getId());
        view.setGameId(gameGeofence.getGameId());
        view.setGeofenceId(gameGeofence.getGeofenceId());
        view.setName(gameGeofence.getName());
        view.setType(gameGeofence.getType());
        view.setRole(gameGeofence.getRole());
        view.setArea(geofence.getArea());
        return view;
    }

    private boolean canReceiveMapUpdate(GameMember member) {
        return GameMember.STATUS_ACTIVE.equals(member.getStatus())
                || GameMember.STATUS_CAUGHT.equals(member.getStatus());
    }

    private boolean canReceivePingUpdate(GameMember member) {
        return GameMember.STATUS_ACTIVE.equals(member.getStatus())
                && GameMember.ROLE_HUNTER.equals(member.getRole());
    }

    private GameMember getMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", memberId),
                        new Condition.Equals("gameId", gameId))));
    }

    private List<GameMember> getMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    private Player getPlayer(long playerId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
    }

}
