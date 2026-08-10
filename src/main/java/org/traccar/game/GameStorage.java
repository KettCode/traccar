package org.traccar.game;

import jakarta.inject.Inject;
import org.traccar.model.Game;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
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

    public List<Game> getRunningGames() throws StorageException {
        return storage.getObjects(Game.class, new Request(
                new Columns.All(), new Condition.Equals("status", Game.STATUS_RUNNING), new Order("id")));
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
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("role", GameMember.ROLE_HUNTED)),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
    }

    public List<GameMember> getActiveHunterMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("role", GameMember.ROLE_HUNTER)),
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

    public List<GamePing> getGamePingsByScheduledAt(long gameId, Date scheduledAt) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("scheduledAt", scheduledAt)), new Order("id")));
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

    private Condition addOrEquals(Condition condition, String column, long value) {
        Condition equals = new Condition.Equals(column, value);
        if (condition == null) {
            return equals;
        }
        return new Condition.Or(condition, equals);
    }

}
