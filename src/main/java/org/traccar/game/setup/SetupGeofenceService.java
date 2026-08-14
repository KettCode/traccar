package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.helper.LogAction;
import org.traccar.model.GameGeofence;
import org.traccar.model.Geofence;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class SetupGeofenceService {

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    public boolean removeGeofence(long userId, long geofenceId, HttpServletRequest request) throws Exception {
        permissionsService.checkPermission(Geofence.class, userId, geofenceId);
        permissionsService.checkEdit(userId, Geofence.class, false, false);

        Geofence geofence = storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", geofenceId)));
        if (geofence == null) {
            return false;
        }

        if (isReferenced(geofenceId)) {
            throw new IllegalArgumentException("Geofence is used by a game and cannot be deleted");
        }

        storage.removeObject(Geofence.class, new Request(new Condition.Equals("id", geofenceId)));
        cacheManager.invalidateObject(true, Geofence.class, geofenceId, ObjectOperation.DELETE);
        actionLogger.remove(request, userId, Geofence.class, geofenceId);

        return true;
    }

    private boolean isReferenced(long geofenceId) throws StorageException {
        return storage.getObject(GameGeofence.class, new Request(
                new Columns.Include("id"), new Condition.Equals("geofenceId", geofenceId))) != null;
    }

}
