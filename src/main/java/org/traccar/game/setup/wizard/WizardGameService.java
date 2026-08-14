package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.game.GamePermissionService;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Date;

public class WizardGameService {

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GamePermissionService gamePermissionService;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    public Game createDraftGame(long userId, Game entity, HttpServletRequest request) throws Exception {
        if (entity == null || entity.getName() == null || entity.getName().isBlank()) {
            throw new IllegalArgumentException("Game name is required");
        }
        validator.validateSettings(entity);

        Game game = new Game();
        game.setName(entity.getName().trim());
        game.setStatus(Game.STATUS_DRAFT);
        game.setPingIntervalSeconds(entity.getPingIntervalSeconds());
        game.setSpeedhuntLimit(entity.getSpeedhuntLimit());
        game.setSpeedhuntPingLimit(entity.getSpeedhuntPingLimit());
        game.setAllowConsecutiveSpeedhuntsSameTarget(entity.getAllowConsecutiveSpeedhuntsSameTarget());
        game.setLocationReminderEnabled(entity.getLocationReminderEnabled());
        game.setMaxPositionAgeSeconds(entity.getMaxPositionAgeSeconds());
        game.setLocationReminderIntervalSeconds(entity.getLocationReminderIntervalSeconds());
        game.setPlannedEndAt(entity.getPlannedEndAt());
        game.setCreatedAt(new Date());

        permissionsService.checkEdit(userId, game, true, false);

        game.setId(storage.addObject(game, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, game);

        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(request, userId, userId, Game.class, game.getId());
        }

        return game;
    }

    public Game updateSettings(
            long userId, long gameId, Game settings, HttpServletRequest request) throws Exception {
        if (settings == null) {
            throw new IllegalArgumentException("Game settings are required");
        }
        if (settings.getName() == null || settings.getName().isBlank()) {
            throw new IllegalArgumentException("Game name is required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }

        validator.validateSettings(settings);
        Game update = new Game();
        update.setId(gameId);
        update.setName(settings.getName().trim());
        update.setPingIntervalSeconds(settings.getPingIntervalSeconds());
        update.setSpeedhuntLimit(settings.getSpeedhuntLimit());
        update.setSpeedhuntPingLimit(settings.getSpeedhuntPingLimit());
        update.setAllowConsecutiveSpeedhuntsSameTarget(settings.getAllowConsecutiveSpeedhuntsSameTarget());
        update.setLocationReminderEnabled(settings.getLocationReminderEnabled());
        update.setMaxPositionAgeSeconds(settings.getMaxPositionAgeSeconds());
        update.setLocationReminderIntervalSeconds(settings.getLocationReminderIntervalSeconds());
        update.setPlannedEndAt(settings.getPlannedEndAt());
        update.setUpdatedAt(new Date());

        storage.updateObject(update, new Request(
                new Columns.Include(
                        "name",
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
        actionLogger.edit(request, userId, update);

        return game;
    }

    public boolean removeGame(long userId, long gameId, HttpServletRequest request) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        storage.removeObject(Game.class, new Request(new Condition.Equals("id", gameId)));
        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.DELETE);
        actionLogger.remove(request, userId, Game.class, gameId);

        return true;
    }

    public void copySettings(Game source, Game target) {
        target.setPingIntervalSeconds(source.getPingIntervalSeconds());
        target.setSpeedhuntLimit(source.getSpeedhuntLimit());
        target.setSpeedhuntPingLimit(source.getSpeedhuntPingLimit());
        target.setAllowConsecutiveSpeedhuntsSameTarget(source.getAllowConsecutiveSpeedhuntsSameTarget());
        target.setLocationReminderEnabled(source.getLocationReminderEnabled());
        target.setMaxPositionAgeSeconds(source.getMaxPositionAgeSeconds());
        target.setLocationReminderIntervalSeconds(source.getLocationReminderIntervalSeconds());
    }

    public Game createCopiedDraftGame(
            long userId, Game source, String name, boolean copySettings,
            HttpServletRequest request) throws Exception {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Game name is required");
        }

        Game game = new Game();
        game.setName(name.trim());
        game.setStatus(Game.STATUS_DRAFT);
        game.setCreatedAt(new Date());
        if (copySettings) {
            copySettings(source, game);
        }

        permissionsService.checkEdit(userId, game, true, false);

        game.setId(storage.addObject(game, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, game);

        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(request, userId, userId, Game.class, game.getId());
        }

        return game;
    }

}
