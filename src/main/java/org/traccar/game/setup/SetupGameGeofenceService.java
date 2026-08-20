package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.Geofence;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

public class SetupGameGeofenceService {

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private SetupStorage setupStorage;

    public boolean addGeofences(
            long userId, long gameId, List<GameGeofence> requests,
            HttpServletRequest httpRequest) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Game geofences are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        for (GameGeofence request : requests) {
            linkGeofence(userId, game, request, httpRequest);
        }
        return true;
    }

    public boolean updateGeofence(
            long userId, long gameId, long gameGeofenceId, GameGeofence request,
            HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        if (request == null) {
            throw new IllegalArgumentException("Game geofence is required");
        }

        GameGeofence gameGeofence = setupStorage.getGameGeofence(gameId, gameGeofenceId);
        if (gameGeofence == null) {
            return false;
        }
        String name = normalizeName(request.getName(), gameGeofence.getName());
        String type = normalizeType(request.getType(), gameGeofence.getType());
        String role = normalizeRole(request.getRole());

        GameGeofence update = new GameGeofence();
        update.setId(gameGeofenceId);
        update.setName(name);
        update.setType(type);
        update.setRole(role);
        update.setActive(request.getActive());
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("name", "type", "role", "active", "updatedAt"),
                new Condition.Equals("id", gameGeofenceId)));
        cacheManager.invalidateObject(true, GameGeofence.class, gameGeofenceId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        return true;
    }

    public boolean removeGeofence(
            long userId, long gameId, long gameGeofenceId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        GameGeofence gameGeofence = setupStorage.getGameGeofence(gameId, gameGeofenceId);
        if (gameGeofence == null) {
            return false;
        }

        storage.removeObject(GameGeofence.class, new Request(new Condition.Equals("id", gameGeofenceId)));
        cacheManager.invalidateObject(true, GameGeofence.class, gameGeofenceId, ObjectOperation.DELETE);
        actionLogger.remove(httpRequest, userId, GameGeofence.class, gameGeofenceId);

        return true;
    }

    public void copyActiveGeofences(
            long userId, long sourceGameId, Game targetGame,
            HttpServletRequest httpRequest) throws Exception {
        gameService.getEditableDraftGame(userId, targetGame.getId());

        var gameGeofences = setupStorage.getActiveGameGeofences(sourceGameId);
        for (GameGeofence gameGeofence : gameGeofences) {
            permissionsService.checkPermission(Geofence.class, userId, gameGeofence.getGeofenceId());
            GameGeofence targetGameGeofence = new GameGeofence();
            targetGameGeofence.setGameId(targetGame.getId());
            targetGameGeofence.setGeofenceId(gameGeofence.getGeofenceId());
            targetGameGeofence.setName(gameGeofence.getName());
            targetGameGeofence.setType(gameGeofence.getType());
            targetGameGeofence.setRole(gameGeofence.getRole());
            targetGameGeofence.setActive(true);
            targetGameGeofence.setCreatedAt(new Date());
            targetGameGeofence.setId(storage.addObject(targetGameGeofence, new Request(new Columns.Exclude("id"))));
            actionLogger.create(httpRequest, userId, targetGameGeofence);
        }
    }

    private void linkGeofence(
            long userId, Game game, GameGeofence request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null || request.getGeofenceId() == 0) {
            throw new IllegalArgumentException("Geofence is required");
        }
        permissionsService.checkPermission(Geofence.class, userId, request.getGeofenceId());

        Geofence geofence = setupStorage.getGeofence(request.getGeofenceId());
        if (geofence == null) {
            throw new IllegalArgumentException("Referenced geofence is missing");
        }

        String name = normalizeName(request.getName(), geofence.getName());
        String type = normalizeType(request.getType(), GameGeofence.TYPE_PLAYFIELD);
        String role = normalizeRole(request.getRole());

        GameGeofence gameGeofence = new GameGeofence();
        gameGeofence.setGameId(game.getId());
        gameGeofence.setGeofenceId(geofence.getId());
        gameGeofence.setName(name);
        gameGeofence.setType(type);
        gameGeofence.setRole(role);
        gameGeofence.setActive(request.getActive());
        gameGeofence.setCreatedAt(new Date());
        gameGeofence.setId(storage.addObject(gameGeofence, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, gameGeofence);
    }

    private String normalizeName(String name, String defaultName) {
        String result = name != null ? name.trim() : null;
        if (result == null || result.isEmpty()) {
            result = defaultName;
        }
        return result;
    }

    private String normalizeType(String type, String defaultType) {
        String result = type != null && !type.isBlank() ? type.trim() : defaultType;
        validator.validateGeofenceType(result);
        return result;
    }

    private String normalizeRole(String role) {
        String result = role != null && !role.isBlank() ? role.trim() : null;
        if (result != null) {
            validator.validateRole(result);
        }
        return result;
    }

}
