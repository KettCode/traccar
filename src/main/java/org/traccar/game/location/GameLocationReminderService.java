package org.traccar.game.location;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.notification.GamePushNotificationService;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameLocationReminderService {

    private final Map<Long, Long> lastCheckMillisByGameId = new HashMap<>();

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GamePushNotificationService pushNotificationService;

    public void runDueReminders() throws Exception {
        Date now = new Date();
        List<Game> runningGames = gameStorage.getRunningGames();
        Set<Long> runningGameIds = runningGames.stream().map(Game::getId).collect(Collectors.toSet());
        lastCheckMillisByGameId.keySet().retainAll(runningGameIds);

        for (Game game : runningGames) {
            if (!shouldCheckGame(game, now)) {
                lastCheckMillisByGameId.remove(game.getId());
                continue;
            }
            if (!isCheckDue(game, now)) {
                continue;
            }
            checkGame(game, now);
            lastCheckMillisByGameId.put(game.getId(), now.getTime());
        }
    }

    private boolean shouldCheckGame(Game game, Date now) {
        return game.getLocationReminderEnabled()
                && game.getLocationReminderIntervalSeconds() > 0
                && game.getStartedAt() != null
                && !now.before(game.getStartedAt());
    }

    private boolean isCheckDue(Game game, Date now) {
        Long lastCheckMillis = lastCheckMillisByGameId.get(game.getId());
        return lastCheckMillis == null
                || lastCheckMillis <= now.getTime() - game.getLocationReminderIntervalSeconds() * 1000L;
    }

    private void checkGame(Game game, Date now) throws Exception {
        List<GameMember> targets = gameStorage.getActiveHuntedMembers(game.getId());
        if (targets.isEmpty()) {
            return;
        }

        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(targets);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(playersById.values()
                .stream()
                .map(Player::getDeviceId)
                .filter(deviceId -> deviceId != 0)
                .collect(Collectors.toSet()));
        var dueTargets = new ArrayList<GameMember>();
        for (GameMember target : targets) {
            Player player = playersById.get(target.getPlayerId());
            Position position = player != null && player.getDeviceId() != 0
                    ? latestPositionsByDeviceId.get(player.getDeviceId()) : null;
            if (isLocationMissingOrStale(game, position, now) && markLocationReminderIfDue(game, target, now)) {
                dueTargets.add(target);
            }
        }

        if (!dueTargets.isEmpty()) {
            pushNotificationService.notifyOwnLocationMissing(dueTargets);
            pushNotificationService.notifyLocationsMissingForManagement(game.getId(), dueTargets);
        }
    }

    private boolean isLocationMissingOrStale(Game game, Position position, Date now) {
        if (position == null || position.getFixTime() == null) {
            return true;
        }
        return game.getMaxPositionAgeSeconds() > 0
                && position.getFixTime().before(new Date(now.getTime() - game.getMaxPositionAgeSeconds() * 1000L));
    }

    private boolean markLocationReminderIfDue(Game game, GameMember target, Date now) throws Exception {
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

}
