package org.traccar.game;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
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
    private GameDevicePermissionService devicePermissionService;

    public Game activate(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }

        validator.validateSettings(game);
        devicePermissionService.validateActiveParticipants(gameId);
        devicePermissionService.syncGameDevicePermissions(userId, gameId, httpRequest);

        Game update = new Game();
        update.setId(gameId);
        update.setStatus(Game.STATUS_RUNNING);
        update.setStartedAt(new Date());
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "startedAt", "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

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

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

}
