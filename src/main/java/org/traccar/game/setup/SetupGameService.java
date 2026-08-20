package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.game.GamePermissionService;
import org.traccar.game.GameService;
import org.traccar.game.GameStorage;
import org.traccar.game.GameValidatorService;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.setup.request.SetupCopyRequest;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.ObjectOperation;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SetupGameService {

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
    private GameStorage gameStorage;

    @Inject
    private GameValidatorService validator;

    @Inject
    private GameNotificationService notificationService;

    @Inject
    private SetupGameMemberService gameMemberService;

    @Inject
    private SetupGameGeofenceService gameGeofenceService;

    public List<Game> getGames(long userId) throws StorageException {
        var conditions = new ArrayList<Condition>();
        if (permissionsService.notAdmin(userId)) {
            conditions.add(new Condition.Permission(User.class, userId, Game.class));
        }
        return storage.getObjects(Game.class, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("id")));
    }

    public Game createDraftGame(long userId, Game entity, HttpServletRequest request) throws Exception {
        if (entity == null) {
            throw new IllegalArgumentException("Game is required");
        }
        validator.validateSettings(entity);

        Game game = new Game();
        game.setName(normalizeRequiredName(entity.getName()));
        game.setStatus(Game.STATUS_DRAFT);
        applySetupSettings(entity, game);
        game.setStartedAt(entity.getStartedAt());
        game.setPlannedEndAt(entity.getPlannedEndAt());
        return addDraftGame(userId, game, request);
    }

    public Game updateSettings(
            long userId, long gameId, Game settings, HttpServletRequest request) throws Exception {
        if (settings == null) {
            throw new IllegalArgumentException("Game settings are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }

        validator.validateSettings(settings);
        Game update = new Game();
        update.setId(gameId);
        update.setName(normalizeRequiredName(settings.getName()));
        applySetupSettings(settings, update);
        update.setStartedAt(settings.getStartedAt());
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
                        "startedAt",
                        "plannedEndAt",
                        "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        notificationService.notifyGameMembers(gameId, notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_GAME_SETTINGS_CHANGED, true));

        return game;
    }

    public boolean removeGame(long userId, long gameId, HttpServletRequest request) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        List<GameMember> members = gameStorage.getGameMembers(gameId);

        storage.removeObject(Game.class, new Request(new Condition.Equals("id", gameId)));
        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.DELETE);
        actionLogger.remove(request, userId, Game.class, gameId);

        notificationService.notifyMembers(members, notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_GAME_DELETED));

        return true;
    }

    public Game copyGame(
            long userId, long sourceGameId, SetupCopyRequest request,
            HttpServletRequest httpRequest) throws Exception {
        Game source = gameService.getAccessibleGame(userId, sourceGameId);
        if (source == null) {
            return null;
        }

        String name = request != null ? normalizeOptionalName(request.getName()) : null;
        if (name == null) {
            name = source.getName() + " Copy";
        }

        Game game = createCopiedDraftGame(
                userId, source, name, request == null || request.getCopySettings(), httpRequest);

        if (request == null || request.getCopyMembers()) {
            gameMemberService.copyMembers(userId, source.getId(), game, httpRequest);
        }
        if (request == null || request.getCopyGeofences()) {
            gameGeofenceService.copyGeofences(userId, source.getId(), game, httpRequest);
        }

        return game;
    }

    public void copySettings(Game source, Game target) {
        applySetupSettings(source, target);
    }

    private void applySetupSettings(Game source, Game target) {
        target.setPingIntervalSeconds(source.getPingIntervalSeconds());
        target.setSpeedhuntLimit(source.getSpeedhuntLimit());
        target.setSpeedhuntPingLimit(source.getSpeedhuntPingLimit());
        target.setAllowConsecutiveSpeedhuntsSameTarget(source.getAllowConsecutiveSpeedhuntsSameTarget());
        target.setLocationReminderEnabled(source.getLocationReminderEnabled());
        target.setMaxPositionAgeSeconds(source.getMaxPositionAgeSeconds());
        target.setLocationReminderIntervalSeconds(source.getLocationReminderIntervalSeconds());
        target.setStartedAt(source.getStartedAt());
        target.setPlannedEndAt(source.getPlannedEndAt());
    }

    private Game createCopiedDraftGame(
            long userId, Game source, String name, boolean copySettings,
            HttpServletRequest request) throws Exception {
        Game game = new Game();
        game.setName(normalizeRequiredName(name));
        game.setStatus(Game.STATUS_DRAFT);
        if (copySettings) {
            copySettings(source, game);
        }
        return addDraftGame(userId, game, request);
    }

    private Game addDraftGame(long userId, Game game, HttpServletRequest request) throws Exception {
        game.setCreatedAt(new Date());
        permissionsService.checkEdit(userId, game, true, false);

        game.setId(storage.addObject(game, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, game);

        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(request, userId, userId, Game.class, game.getId());
        }

        return game;
    }

    private String normalizeRequiredName(String name) {
        String result = normalizeOptionalName(name);
        if (result == null) {
            throw new IllegalArgumentException("Game name is required");
        }
        return result;
    }

    private String normalizeOptionalName(String name) {
        String result = name != null ? name.trim() : null;
        return result != null && !result.isEmpty() ? result : null;
    }

}
