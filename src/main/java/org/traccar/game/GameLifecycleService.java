package org.traccar.game;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.request.GameRuntimeSettingsRequest;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.ObjectOperation;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import org.traccar.session.cache.CacheManager;

import java.util.Date;

public class GameLifecycleService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameDevicePermissionService devicePermissionService;

    @Inject
    private GameNotificationService notificationService;

    public Game activate(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }

        validator.validateSettings(game);
        devicePermissionService.validateAndSyncGameDevicePermissions(userId, gameId, httpRequest);

        Date now = new Date();
        Game update = new Game();
        update.setId(gameId);
        update.setStatus(Game.STATUS_RUNNING);
        update.setStartedAt(game.getStartedAt() != null ? game.getStartedAt() : now);
        update.setActivatedAt(now);
        update.setUpdatedAt(now);
        storage.updateObject(update, new Request(
                new Columns.Include("status", "startedAt", "activatedAt", "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        notificationService.notifyGameMembers(gameId, notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_GAME_ACTIVATED, true));

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    public Game finish(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableRunningGame(userId, gameId);
        if (game == null) {
            return null;
        }

        devicePermissionService.clearGameDevicePermissions(userId, gameId, httpRequest);

        Game update = new Game();
        update.setId(gameId);
        update.setStatus(Game.STATUS_FINISHED);
        update.setFinishedAt(new Date());
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "finishedAt", "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        notificationService.notifyGameMembers(gameId, notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_GAME_FINISHED, true));

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    public Game updateRuntimeSettings(
            long userId, long gameId, GameRuntimeSettingsRequest settings, HttpServletRequest httpRequest)
            throws Exception {
        if (settings == null) {
            throw new IllegalArgumentException("Runtime settings are required");
        }

        GameRuntimeContext context = runtimePermissionService.requireCanManageRuntimeSettings(userId, gameId);
        if (context == null) {
            return null;
        }

        Game merged = mergeRuntimeSettings(context.game(), settings);
        validator.validateSettings(merged);

        Date now = new Date();
        Game update = new Game();
        update.setId(gameId);
        update.setPingIntervalSeconds(merged.getPingIntervalSeconds());
        update.setSpeedhuntLimit(merged.getSpeedhuntLimit());
        update.setSpeedhuntPingLimit(merged.getSpeedhuntPingLimit());
        update.setAllowConsecutiveSpeedhuntsSameTarget(merged.getAllowConsecutiveSpeedhuntsSameTarget());
        update.setLocationReminderEnabled(merged.getLocationReminderEnabled());
        update.setMaxPositionAgeSeconds(merged.getMaxPositionAgeSeconds());
        update.setLocationReminderIntervalSeconds(merged.getLocationReminderIntervalSeconds());
        update.setPlannedEndAt(merged.getPlannedEndAt());
        update.setUpdatedAt(now);

        storage.updateObject(update, new Request(
                new Columns.Include(
                        "pingIntervalSeconds",
                        "speedhuntLimit",
                        "speedhuntPingLimit",
                        "allowConsecutiveSpeedhuntsSameTarget",
                        "locationReminderEnabled",
                        "maxPositionAgeSeconds",
                        "locationReminderIntervalSeconds",
                        "plannedEndAt",
                        "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        notificationService.notifyGameMembers(gameId, notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_GAME_SETTINGS_CHANGED, true));

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    private Game mergeRuntimeSettings(Game current, GameRuntimeSettingsRequest settings) {
        Game merged = new Game();
        merged.setPingIntervalSeconds(valueOrCurrent(
                settings.getPingIntervalSeconds(), current.getPingIntervalSeconds()));
        merged.setSpeedhuntLimit(valueOrCurrent(settings.getSpeedhuntLimit(), current.getSpeedhuntLimit()));
        merged.setSpeedhuntPingLimit(valueOrCurrent(settings.getSpeedhuntPingLimit(), current.getSpeedhuntPingLimit()));
        merged.setAllowConsecutiveSpeedhuntsSameTarget(valueOrCurrent(
                settings.getAllowConsecutiveSpeedhuntsSameTarget(),
                current.getAllowConsecutiveSpeedhuntsSameTarget()));
        merged.setLocationReminderEnabled(valueOrCurrent(
                settings.getLocationReminderEnabled(), current.getLocationReminderEnabled()));
        merged.setMaxPositionAgeSeconds(valueOrCurrent(
                settings.getMaxPositionAgeSeconds(), current.getMaxPositionAgeSeconds()));
        merged.setLocationReminderIntervalSeconds(valueOrCurrent(
                settings.getLocationReminderIntervalSeconds(), current.getLocationReminderIntervalSeconds()));
        merged.setPlannedEndAt(settings.getPlannedEndAtSet() ? settings.getPlannedEndAt() : current.getPlannedEndAt());
        return merged;
    }

    private int valueOrCurrent(Integer value, int current) {
        return value != null ? value : current;
    }

    private boolean valueOrCurrent(Boolean value, boolean current) {
        return value != null ? value : current;
    }

}
