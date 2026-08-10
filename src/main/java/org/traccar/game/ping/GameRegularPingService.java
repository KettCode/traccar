package org.traccar.game.ping;

import jakarta.inject.Inject;
import org.traccar.game.map.GameMapUpdateService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameRegularPingService {

    private static final long SYSTEM_USER_ID = 0;

    private final Map<Long, RegularPingScheduleState> scheduleStates = new HashMap<>();

    @Inject
    private Storage storage;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GamePingService pingService;

    @Inject
    private GameMapUpdateService mapUpdateService;

    public void runDuePings() throws Exception {
        Date now = new Date();
        List<Game> runningGames = getRunningGames();
        scheduleStates.keySet().retainAll(runningGames.stream().map(Game::getId).collect(Collectors.toSet()));

        for (Game game : runningGames) {
            RegularPingSlot slot = getCurrentRegularPingSlot(game, now);
            if (slot != null && !isRegularPingSlotProcessed(game, slot)) {
                if (createRegularPings(game, slot, now)) {
                    markRegularPingSlotProcessed(game, slot);
                }
            }
        }
    }

    private boolean createRegularPings(Game game, RegularPingSlot slot, Date now) throws Exception {
        List<GameMember> targets = getActiveHuntedMembers(game.getId());
        if (targets.isEmpty()) {
            return false;
        }

        Set<Long> completedMemberIds = getCompletedMemberIds(game.getId(), slot.scheduledAt());
        if (!completedMemberIds.isEmpty()) {
            return true;
        }

        Map<Long, Player> playersById = getPlayersById(targets);
        Map<Long, Position> latestPositionsByDeviceId = getLatestPositionsByDeviceId(playersById.values().stream()
                .map(Player::getDeviceId)
                .filter(deviceId -> deviceId != 0)
                .collect(Collectors.toSet()));
        Map<Long, GamePendingEffect> effectsByMemberId = pingService.getNextPendingEffects(
                game.getId(), targets.stream().map(GameMember::getId).collect(Collectors.toSet()));
        var createdPings = new ArrayList<GamePing>();

        for (GameMember target : targets) {
            if (completedMemberIds.contains(target.getId())) {
                continue;
            }

            GamePing ping = createPing(game, target, slot, now);
            GamePendingEffect effect = effectsByMemberId.get(target.getId());
            if (effect != null) {
                pingService.applyPendingEffect(ping, effect, GamePing.SOURCE_REGULAR);
            } else if (!applyLatestPosition(game, target, ping, playersById, latestPositionsByDeviceId)) {
                continue;
            }

            ping.setId(storage.addObject(ping, new Request(new Columns.Exclude("id"))));
            actionLogger.create(null, SYSTEM_USER_ID, ping);

            if (effect != null) {
                pingService.consumeEffect(SYSTEM_USER_ID, effect, ping, null);
            }

            createdPings.add(ping);
        }

        if (!createdPings.isEmpty()) {
            mapUpdateService.notifyRegularPingsCreated(createdPings);
        }
        return !createdPings.isEmpty();
    }

    private boolean applyLatestPosition(
            Game game, GameMember target, GamePing ping, Map<Long, Player> playersById,
            Map<Long, Position> latestPositionsByDeviceId) {
        Player player = playersById.get(target.getPlayerId());
        if (player == null || player.getDeviceId() == 0) {
            return false;
        }
        return pingService.applyPositionIfValid(
                game, ping, latestPositionsByDeviceId.get(player.getDeviceId()), GamePing.SOURCE_REGULAR);
    }

    private GamePing createPing(Game game, GameMember target, RegularPingSlot slot, Date now) {
        GamePing ping = new GamePing();
        ping.setGameId(game.getId());
        ping.setTargetMemberId(target.getId());
        ping.setSource(GamePing.SOURCE_REGULAR);
        ping.setScheduledAt(slot.scheduledAt());
        ping.setCreatedAt(now);
        return ping;
    }

    private RegularPingSlot getCurrentRegularPingSlot(Game game, Date now) {
        if (game.getPingIntervalSeconds() <= 0 || game.getStartedAt() == null || now.before(game.getStartedAt())) {
            return null;
        }
        long elapsedMillis = now.getTime() - game.getStartedAt().getTime();
        long intervalMillis = game.getPingIntervalSeconds() * 1000L;
        long slotIndex = elapsedMillis / intervalMillis;
        long scheduledAtMillis = game.getStartedAt().getTime() + slotIndex * intervalMillis;
        return new RegularPingSlot(new Date(scheduledAtMillis));
    }

    private boolean isRegularPingSlotProcessed(Game game, RegularPingSlot slot) {
        RegularPingScheduleState state = scheduleStates.get(game.getId());
        return state != null
                && state.startedAtMillis() == game.getStartedAt().getTime()
                && state.pingIntervalSeconds() == game.getPingIntervalSeconds()
                && state.processedScheduledAtMillis() == slot.scheduledAt().getTime();
    }

    private void markRegularPingSlotProcessed(Game game, RegularPingSlot slot) {
        scheduleStates.put(game.getId(), new RegularPingScheduleState(
                game.getStartedAt().getTime(), game.getPingIntervalSeconds(), slot.scheduledAt().getTime()));
    }

    private List<Game> getRunningGames() throws StorageException {
        return storage.getObjects(Game.class, new Request(
                new Columns.All(), new Condition.Equals("status", Game.STATUS_RUNNING), new Order("id")));
    }

    private List<GameMember> getActiveHuntedMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("role", GameMember.ROLE_HUNTED)),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
    }

    private Set<Long> getCompletedMemberIds(long gameId, Date scheduledAt) throws StorageException {
        Set<Long> result = new HashSet<>();
        var pings = storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("scheduledAt", scheduledAt)), new Order("id")));
        for (GamePing ping : pings) {
            result.add(ping.getTargetMemberId());
        }
        return result;
    }

    private Map<Long, Player> getPlayersById(List<GameMember> members) throws StorageException {
        Condition condition = null;
        for (GameMember member : members) {
            condition = addOrEquals(condition, "id", member.getPlayerId());
        }
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

    private Map<Long, Position> getLatestPositionsByDeviceId(Set<Long> deviceIds) throws StorageException {
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

    private Condition addOrEquals(Condition condition, String column, long value) {
        Condition equals = new Condition.Equals(column, value);
        if (condition == null) {
            return equals;
        }
        return new Condition.Or(condition, equals);
    }

    private record RegularPingSlot(Date scheduledAt) {
    }

    private record RegularPingScheduleState(
            long startedAtMillis, int pingIntervalSeconds, long processedScheduledAtMillis) {
    }

}
