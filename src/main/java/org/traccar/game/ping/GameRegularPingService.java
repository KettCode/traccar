package org.traccar.game.ping;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.map.GameMapUpdateService;
import org.traccar.game.notification.GamePushNotificationService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
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
    private CacheManager cacheManager;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GamePingService pingService;

    @Inject
    private GameMapUpdateService mapUpdateService;

    @Inject
    private GamePushNotificationService pushNotificationService;

    public void runDuePings() throws Exception {
        Date now = new Date();
        List<Game> runningGames = gameStorage.getRunningGames();
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
        List<GameMember> targets = gameStorage.getActiveHuntedMembers(game.getId());
        if (targets.isEmpty()) {
            return false;
        }

        Set<Long> completedMemberIds = getCompletedMemberIds(game.getId(), slot.scheduledAt());
        if (!completedMemberIds.isEmpty()) {
            return true;
        }

        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(targets);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(playersById.values().stream()
                .map(Player::getDeviceId)
                .filter(deviceId -> deviceId != 0)
                .collect(Collectors.toSet()));
        Map<Long, GamePendingEffect> effectsByMemberId = pingService.getNextPendingEffects(
                game.getId(), targets.stream().map(GameMember::getId).collect(Collectors.toSet()));
        var createdPings = new ArrayList<GamePing>();
        var dueMissingLocationTargets = new ArrayList<GameMember>();

        for (GameMember target : targets) {
            if (completedMemberIds.contains(target.getId())) {
                continue;
            }

            GamePing ping = createPing(game, target, slot, now);
            GamePendingEffect effect = effectsByMemberId.get(target.getId());
            if (effect != null) {
                pingService.applyPendingEffect(ping, effect, GamePing.SOURCE_REGULAR);
            } else {
                Player player = playersById.get(target.getPlayerId());
                Position position = player != null && player.getDeviceId() != 0
                        ? latestPositionsByDeviceId.get(player.getDeviceId()) : null;
                boolean locationMissingOrStale = position == null || !pingService.isPositionValid(game, position);
                if (position != null) {
                    pingService.applyPosition(ping, position, GamePing.SOURCE_REGULAR);
                }
                if (locationMissingOrStale) {
                    if (markLocationReminderIfDue(game, target, now)) {
                        dueMissingLocationTargets.add(target);
                    }
                    if (position == null) {
                        continue;
                    }
                }
            }

            ping.setId(storage.addObject(ping, new Request(new Columns.Exclude("id"))));
            pingService.markLastVisiblePing(ping);
            actionLogger.create(null, SYSTEM_USER_ID, ping);

            if (effect != null) {
                pingService.consumeEffect(SYSTEM_USER_ID, effect, ping, null);
            }

            createdPings.add(ping);
        }

        if (!createdPings.isEmpty()) {
            mapUpdateService.notifyRegularPingsCreated(createdPings);
            pushNotificationService.notifyRegularPingsCreated(game.getId());
        }
        if (!dueMissingLocationTargets.isEmpty()) {
            pushNotificationService.notifyOwnLocationMissing(dueMissingLocationTargets);
            pushNotificationService.notifyRegularPingLocationsMissing(game.getId(), dueMissingLocationTargets);
        }
        return !createdPings.isEmpty();
    }

    private boolean markLocationReminderIfDue(Game game, GameMember target, Date now) throws Exception {
        if (!game.getLocationReminderEnabled() || game.getLocationReminderIntervalSeconds() <= 0) {
            return false;
        }
        if (target.getLastLocationReminderAt() != null
                && target.getLastLocationReminderAt().after(new Date(
                        now.getTime() - game.getLocationReminderIntervalSeconds() * 1000L))) {
            return false;
        }

        GameMember update = new GameMember();
        update.setId(target.getId());
        update.setLastLocationReminderAt(now);
        storage.updateObject(update, new Request(
                new Columns.Include("lastLocationReminderAt"),
                new Condition.Equals("id", target.getId())));
        cacheManager.invalidateObject(true, GameMember.class, target.getId(), ObjectOperation.UPDATE);
        target.setLastLocationReminderAt(now);
        return true;
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

    private Set<Long> getCompletedMemberIds(long gameId, Date scheduledAt) throws StorageException {
        Set<Long> result = new HashSet<>();
        var pings = gameStorage.getRegularGamePingsByScheduledAt(gameId, scheduledAt);
        for (GamePing ping : pings) {
            result.add(ping.getTargetMemberId());
        }
        return result;
    }

    private record RegularPingSlot(Date scheduledAt) {
    }

    private record RegularPingScheduleState(
            long startedAtMillis, int pingIntervalSeconds, long processedScheduledAtMillis) {
    }

}
