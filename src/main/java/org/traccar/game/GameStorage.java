package org.traccar.game;

import jakarta.inject.Inject;
import org.traccar.model.Device;
import org.traccar.model.Game;
import org.traccar.model.GameCatch;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;
import org.traccar.model.GameRevealedPosition;
import org.traccar.model.GameSpeedhunt;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameStorage {

    @Inject
    private Storage storage;

    public Game getGame(long gameId) throws StorageException {
        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    public List<Game> getRunningGames() throws StorageException {
        return storage.getObjects(Game.class, new Request(
                new Columns.All(), new Condition.Equals("status", Game.STATUS_RUNNING), new Order("id")));
    }

    public List<Game> getGamesByMembers(List<GameMember> members) throws StorageException {
        Condition condition = null;
        for (GameMember member : members) {
            condition = addOrEquals(condition, "id", member.getGameId());
        }
        if (condition == null) {
            return List.of();
        }

        return storage.getObjects(Game.class, new Request(new Columns.All(), condition, new Order("id")));
    }

    public GameMember getGameMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", memberId),
                        new Condition.Equals("gameId", gameId))));
    }

    public GameMember getGameMemberByPlayer(long gameId, long playerId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("playerId", playerId))));
    }

    public List<GameMember> getViewableGameMembersByPlayer(long playerId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("playerId", playerId),
                        new Condition.Or(
                                new Condition.Equals("status", GameMember.STATUS_ACTIVE),
                                new Condition.Equals("status", GameMember.STATUS_CAUGHT))), new Order("id")));
    }

    public List<GameMember> getGameMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    public List<GameMember> getActiveGameMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
    }

    public List<GameMember> getGameMembersByRole(long gameId, String role) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("role", role)), new Order("id")));
    }

    public List<GameMember> getActiveHuntedMembers(long gameId) throws StorageException {
        return getActiveGameMembersByRole(gameId, GameMember.ROLE_HUNTED);
    }

    public List<GameMember> getActiveHunterMembers(long gameId) throws StorageException {
        return getActiveGameMembersByRole(gameId, GameMember.ROLE_HUNTER);
    }

    public List<GameMember> getActiveManagementMembers(long gameId) throws StorageException {
        return getActiveGameMembersByRole(gameId, GameMember.ROLE_GAME_MANAGEMENT);
    }

    private List<GameMember> getActiveGameMembersByRole(long gameId, String role) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("role", role)),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
    }

    public List<GameMember> getNonLeftGameMembers(long gameId) throws StorageException {
        return getGameMembers(gameId).stream()
                .filter(member -> !GameMember.STATUS_LEFT.equals(member.getStatus()))
                .toList();
    }

    public GameJoker getGameJoker(long gameId, long jokerId) throws StorageException {
        return storage.getObject(GameJoker.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", jokerId),
                        new Condition.Equals("gameId", gameId))));
    }

    public GameGeofence getGameGeofence(long gameId, long gameGeofenceId) throws StorageException {
        return storage.getObject(GameGeofence.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", gameGeofenceId),
                        new Condition.Equals("gameId", gameId))));
    }

    public List<GameGeofence> getGameGeofences(long gameId) throws StorageException {
        return storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    public List<GameGeofence> getActiveGameGeofencesByType(long gameId, String type) throws StorageException {
        return storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("type", type)),
                        new Condition.Equals("active", true)), new Order("id")));
    }

    public Map<Long, Geofence> getGeofencesByGameGeofences(List<GameGeofence> gameGeofences) throws StorageException {
        Condition condition = null;
        for (GameGeofence gameGeofence : gameGeofences) {
            condition = addOrEquals(condition, "id", gameGeofence.getGeofenceId());
        }
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

    public Geofence getGeofence(long geofenceId) throws StorageException {
        return storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", geofenceId)));
    }

    public GameCatch getGameCatch(long gameId, long catchId) throws StorageException {
        return storage.getObject(GameCatch.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", catchId),
                        new Condition.Equals("gameId", gameId))));
    }

    public GameCatch getActiveGameCatchForMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameCatch.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("caughtMemberId", memberId)),
                        new Condition.Equals("status", GameCatch.STATUS_ACTIVE))));
    }

    public List<GameSpeedhunt> getGameSpeedhunts(long gameId) throws StorageException {
        return storage.getObjects(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("sequenceNumber")));
    }

    public GameSpeedhunt getGameSpeedhunt(long gameId, long speedhuntId) throws StorageException {
        return storage.getObject(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", speedhuntId),
                        new Condition.Equals("gameId", gameId))));
    }

    public GameSpeedhunt getActiveGameSpeedhunt(long gameId) throws StorageException {
        for (GameSpeedhunt speedhunt : getGameSpeedhunts(gameId)) {
            if (speedhunt.getEndedAt() == null) {
                return speedhunt;
            }
        }
        return null;
    }

    public List<GameSpeedhunt> getActiveGameSpeedhuntsForTarget(long gameId, long targetMemberId)
            throws StorageException {
        return storage.getObjects(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("targetMemberId", targetMemberId)), new Order("sequenceNumber"))).stream()
                .filter(speedhunt -> speedhunt.getEndedAt() == null)
                .toList();
    }

    public Player getPlayer(long playerId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
    }

    public Player getPlayerByUser(long userId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("userId", userId)));
    }

    public User getUser(long userId) throws StorageException {
        return storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", userId)));
    }

    public Device getDevice(long deviceId) throws StorageException {
        return storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
    }

    public Map<Long, User> getUsersByMembers(List<GameMember> members) throws StorageException {
        Map<Long, Player> playersById = getPlayersByMembers(members);
        Condition condition = null;
        for (Player player : playersById.values()) {
            if (player.getUserId() != 0) {
                condition = addOrEquals(condition, "id", player.getUserId());
            }
        }

        Map<Long, User> usersById = getUsersByCondition(condition);
        var result = new HashMap<Long, User>();
        for (GameMember member : members) {
            Player player = playersById.get(member.getPlayerId());
            if (player != null) {
                User user = usersById.get(player.getUserId());
                if (user != null) {
                    result.put(member.getId(), user);
                }
            }
        }
        return result;
    }

    public Map<Long, Player> getPlayersByMembers(List<GameMember> members) throws StorageException {
        Condition condition = null;
        for (GameMember member : members) {
            condition = addOrEquals(condition, "id", member.getPlayerId());
        }
        return getPlayersByCondition(condition);
    }

    public Map<Long, Position> getLatestPositionsByDeviceIds(Collection<Long> deviceIds) throws StorageException {
        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<Long, Position>();
        var positions = storage.getObjects(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions()));
        for (Position position : positions) {
            if (deviceIds.contains(position.getDeviceId())) {
                result.put(position.getDeviceId(), position);
            }
        }
        return result;
    }

    public Position getLatestPositionByDeviceId(long deviceId) throws StorageException {
        return storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
    }

    public List<GamePing> getRegularGamePingsByScheduledAt(long gameId, Date scheduledAt) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("scheduledAt", scheduledAt)),
                        new Condition.Equals("source", GamePing.SOURCE_REGULAR)), new Order("id")));
    }

    public List<GamePing> getGamePings(long gameId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    public List<GamePing> getSpeedhuntPings(long gameId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("source", GamePing.SOURCE_SPEEDHUNT)), new Order("id")));
    }

    public GamePing getLatestRegularPing(long gameId, long targetMemberId) throws StorageException {
        var pings = storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.And(
                                        new Condition.Equals("gameId", gameId),
                                        new Condition.Equals("targetMemberId", targetMemberId)),
                                new Condition.Equals("source", GamePing.SOURCE_REGULAR)),
                        new Condition.Equals("skipped", false)), new Order("id", true, 1)));
        return pings.isEmpty() ? null : pings.get(0);
    }

    public GameReveal getHunterLocationRevealByJoker(long gameId, long memberId, long jokerId) throws StorageException {
        var reveals = storage.getObjects(GameReveal.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.And(
                                        new Condition.Equals("gameId", gameId),
                                        new Condition.Equals("memberId", memberId)),
                                new Condition.Equals("jokerId", jokerId)),
                        new Condition.Equals("type", GameReveal.TYPE_HUNTER_LOCATIONS)), new Order("id", true, 1)));
        for (GameReveal reveal : reveals) {
            if (reveal.getInvalidatedAt() == null) {
                return reveal;
            }
        }
        return null;
    }

    public List<GameRevealedPosition> getRevealedPositions(long revealId) throws StorageException {
        return storage.getObjects(GameRevealedPosition.class, new Request(
                new Columns.All(), new Condition.Equals("revealId", revealId), new Order("id")));
    }

    public Map<Long, GamePing> getLastVisiblePingsByMembers(List<GameMember> members) throws StorageException {
        Condition condition = null;
        for (GameMember member : members) {
            if (member.getLastVisiblePingId() != 0) {
                condition = addOrEquals(condition, "id", member.getLastVisiblePingId());
            }
        }
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, GamePing>();
        var pings = storage.getObjects(GamePing.class, new Request(new Columns.All(), condition, new Order("id")));
        for (GamePing ping : pings) {
            result.put(ping.getTargetMemberId(), ping);
        }
        return result;
    }

    private Map<Long, Player> getPlayersByCondition(Condition condition) throws StorageException {
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

    private Map<Long, User> getUsersByCondition(Condition condition) throws StorageException {
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

    private Condition addOrEquals(Condition condition, String column, long value) {
        Condition equals = new Condition.Equals(column, value);
        if (condition == null) {
            return equals;
        }
        return new Condition.Or(condition, equals);
    }

}
