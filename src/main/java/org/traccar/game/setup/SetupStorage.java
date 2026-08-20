package org.traccar.game.setup;

import jakarta.inject.Inject;
import org.traccar.model.Device;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SetupStorage {

    @Inject
    private Storage storage;

    public Player getPlayer(long playerId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
    }

    public GameMember getGameMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", memberId),
                        new Condition.Equals("gameId", gameId))));
    }

    public Geofence getGeofence(long geofenceId) throws StorageException {
        return storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", geofenceId)));
    }

    public GameGeofence getGameGeofence(long gameId, long gameGeofenceId) throws StorageException {
        return storage.getObject(GameGeofence.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", gameGeofenceId),
                        new Condition.Equals("gameId", gameId))));
    }

    public List<GameMember> getActiveMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
    }

    public List<GameMember> getGameMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    public List<GameGeofence> getActiveGameGeofences(long gameId) throws StorageException {
        return storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("active", true)), new Order("id")));
    }

    public List<GameGeofence> getGameGeofences(long gameId) throws StorageException {
        return storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    public Set<Long> getMemberPlayerIds(long gameId) throws StorageException {
        var playerIds = new HashSet<Long>();
        var members = storage.getObjects(GameMember.class, new Request(
                new Columns.Include("playerId"), new Condition.Equals("gameId", gameId)));
        for (GameMember member : members) {
            playerIds.add(member.getPlayerId());
        }
        return playerIds;
    }

    public boolean isPlayerReferenced(long playerId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.Include("id"), new Condition.Equals("playerId", playerId))) != null;
    }

    public Map<Long, Player> getPlayersByMembers(List<GameMember> members) throws StorageException {
        Set<Long> playerIds = new HashSet<>();
        for (GameMember member : members) {
            if (member.getPlayerId() != 0) {
                playerIds.add(member.getPlayerId());
            }
        }
        Condition condition = getIdCondition(playerIds);
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, Player>();
        var players = storage.getObjects(Player.class, new Request(new Columns.All(), condition, new Order("id")));
        for (Player player : players) {
            result.put(player.getId(), player);
        }
        return result;
    }

    public Map<Long, User> getUsersByPlayers(List<Player> players) throws StorageException {
        Set<Long> userIds = new HashSet<>();
        for (Player player : players) {
            if (player.getUserId() != 0) {
                userIds.add(player.getUserId());
            }
        }
        Condition condition = getIdCondition(userIds);
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, User>();
        var users = storage.getObjects(User.class, new Request(new Columns.All(), condition, new Order("id")));
        for (User user : users) {
            result.put(user.getId(), user);
        }
        return result;
    }

    public Map<Long, Device> getDevicesByPlayers(List<Player> players) throws StorageException {
        Set<Long> deviceIds = new HashSet<>();
        for (Player player : players) {
            if (player.getDeviceId() != 0) {
                deviceIds.add(player.getDeviceId());
            }
        }
        Condition condition = getIdCondition(deviceIds);
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, Device>();
        var devices = storage.getObjects(Device.class, new Request(new Columns.All(), condition, new Order("id")));
        for (Device device : devices) {
            result.put(device.getId(), device);
        }
        return result;
    }

    public Map<Long, Geofence> getGeofencesByGameGeofences(List<GameGeofence> gameGeofences)
            throws StorageException {
        Set<Long> geofenceIds = new HashSet<>();
        for (GameGeofence gameGeofence : gameGeofences) {
            if (gameGeofence.getGeofenceId() != 0) {
                geofenceIds.add(gameGeofence.getGeofenceId());
            }
        }
        Condition condition = getIdCondition(geofenceIds);
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, Geofence>();
        var geofences = storage.getObjects(Geofence.class, new Request(new Columns.All(), condition, new Order("id")));
        for (Geofence geofence : geofences) {
            result.put(geofence.getId(), geofence);
        }
        return result;
    }

    private Condition getIdCondition(Set<Long> ids) {
        Condition condition = null;
        for (long id : ids) {
            Condition equals = new Condition.Equals("id", id);
            condition = condition == null ? equals : new Condition.Or(condition, equals);
        }
        return condition;
    }

}
