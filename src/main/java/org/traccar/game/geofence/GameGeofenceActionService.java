package org.traccar.game.geofence;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.GameMapUpdateService;
import org.traccar.helper.LogAction;
import org.traccar.model.GameGeofence;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Date;

public class GameGeofenceActionService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameMapUpdateService mapUpdateService;

    public GameGeofence activateGeofence(
            long userId, long gameId, long gameGeofenceId, HttpServletRequest request) throws Exception {
        return setGeofenceActive(userId, gameId, gameGeofenceId, true, request);
    }

    public GameGeofence deactivateGeofence(
            long userId, long gameId, long gameGeofenceId, HttpServletRequest request) throws Exception {
        return setGeofenceActive(userId, gameId, gameGeofenceId, false, request);
    }

    private GameGeofence setGeofenceActive(
            long userId, long gameId, long gameGeofenceId, boolean active, HttpServletRequest request) throws Exception {
        if (runtimePermissionService.requireGameManagement(userId, gameId) == null) {
            return null;
        }

        GameGeofence gameGeofence = gameStorage.getGameGeofence(gameId, gameGeofenceId);
        if (gameGeofence == null) {
            return null;
        }

        GameGeofence update = new GameGeofence();
        update.setId(gameGeofenceId);
        update.setActive(active);
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("active", "updatedAt"),
                new Condition.Equals("id", gameGeofenceId)));
        cacheManager.invalidateObject(true, GameGeofence.class, gameGeofenceId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        gameGeofence.setActive(active);
        gameGeofence.setUpdatedAt(update.getUpdatedAt());
        if (active) {
            mapUpdateService.notifyGeofenceUpdated(gameGeofence);
        } else {
            mapUpdateService.notifyGeofenceRemoved(gameId, gameGeofenceId);
        }
        return gameGeofence;
    }

}
