package org.traccar.game;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Game;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

public class GamePermissionService {

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ConnectionManager connectionManager;

    @Inject
    private LogAction actionLogger;

    public void checkPermission(long userId, long gameId) throws StorageException {
        permissionsService.checkPermission(Game.class, userId, gameId);
    }

    public void checkEdit(long userId, long gameId) throws StorageException {
        checkPermission(userId, gameId);
        permissionsService.checkEdit(userId, Game.class, false, false);
    }

    public void addPermission(
            HttpServletRequest request, long userId, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) throws Exception {
        if (!storage.getPermissions(User.class, ownerId, propertyClass, propertyId).isEmpty()) {
            return;
        }
        storage.addPermission(new Permission(User.class, ownerId, propertyClass, propertyId));
        cacheManager.invalidatePermission(true, User.class, ownerId, propertyClass, propertyId, true);
        connectionManager.invalidatePermission(true, User.class, ownerId, propertyClass, propertyId, true);
        actionLogger.link(request, userId, User.class, ownerId, propertyClass, propertyId);
    }

    public void removePermission(
            HttpServletRequest request, long userId, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) throws Exception {
        storage.removePermission(new Permission(User.class, ownerId, propertyClass, propertyId));
        cacheManager.invalidatePermission(true, User.class, ownerId, propertyClass, propertyId, false);
        connectionManager.invalidatePermission(true, User.class, ownerId, propertyClass, propertyId, false);
        actionLogger.unlink(request, userId, User.class, ownerId, propertyClass, propertyId);
    }

}
