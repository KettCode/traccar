package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.game.GameService;
import org.traccar.game.setup.request.SetupPasswordRequest;
import org.traccar.game.setup.view.PlayerView;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
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
import java.util.Map;

public class SetupPlayerService {

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private SetupClientService setupClientService;

    @Inject
    private SetupStorage setupStorage;

    @Inject
    private GameService gameService;

    public List<PlayerView> getPlayers(long userId, boolean includeInactive) throws StorageException {
        return getPlayerViews(getPlayerObjects(userId, includeInactive));
    }

    public List<PlayerView> getAvailablePlayers(long userId, long gameId) throws StorageException {
        if (gameService.getEditableDraftGame(userId, gameId) == null) {
            return null;
        }

        var memberPlayerIds = setupStorage.getMemberPlayerIds(gameId);
        var players = new ArrayList<Player>();
        for (Player player : getPlayerObjects(userId, false)) {
            if (!memberPlayerIds.contains(player.getId())) {
                players.add(player);
            }
        }
        return getPlayerViews(players);
    }

    private List<Player> getPlayerObjects(long userId, boolean includeInactive) throws StorageException {
        var conditions = new ArrayList<Condition>();
        if (!includeInactive) {
            conditions.add(new Condition.Equals("active", true));
        }
        if (permissionsService.notAdmin(userId)) {
            conditions.add(new Condition.Permission(User.class, userId, Player.class));
        }
        return storage.getObjects(Player.class, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("id")));
    }

    private List<PlayerView> getPlayerViews(List<Player> players) throws StorageException {
        var result = new ArrayList<PlayerView>();
        Map<Long, User> usersById = setupStorage.getUsersByPlayers(players);
        Map<Long, Device> devicesById = setupStorage.getDevicesByPlayers(players);
        for (Player player : players) {
            result.add(toPlayerView(player, usersById, devicesById));
        }
        return result;
    }

    public boolean removePlayer(long userId, long playerId, HttpServletRequest request) throws Exception {
        permissionsService.checkPermission(Player.class, userId, playerId);
        permissionsService.checkEdit(userId, Player.class, false, false);

        Player player = setupStorage.getPlayer(playerId);
        if (player == null) {
            return false;
        }

        if (setupStorage.isPlayerReferenced(playerId)) {
            Player update = new Player();
            update.setId(playerId);
            update.setActive(false);
            update.setUpdatedAt(new Date());
            storage.updateObject(update, new Request(
                    new Columns.Include("active", "updatedAt"),
                    new Condition.Equals("id", playerId)));
            cacheManager.invalidateObject(true, Player.class, playerId, ObjectOperation.UPDATE);
            actionLogger.edit(request, userId, update);
        } else {
            storage.removeObject(Player.class, new Request(new Condition.Equals("id", playerId)));
            cacheManager.invalidateObject(true, Player.class, playerId, ObjectOperation.DELETE);
            actionLogger.remove(request, userId, Player.class, playerId);
        }

        return true;
    }

    public boolean updatePassword(
            long userId, long playerId, SetupPasswordRequest request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Password is required");
        }

        permissionsService.checkPermission(Player.class, userId, playerId);
        Player player = setupStorage.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        if (player.getUserId() == 0) {
            throw new IllegalArgumentException("Player user assignment is missing");
        }

        permissionsService.checkUser(userId, player.getUserId());
        updateUserPassword(userId, player.getUserId(), request.getPassword(), httpRequest);
        return true;
    }

    private void updateUserPassword(
            long userId, long playerUserId, String password,
            HttpServletRequest httpRequest) throws Exception {
        validatePassword(password);
        User user = new User();
        user.setId(playerUserId);
        user.setPassword(password);
        storage.updateObject(user, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", playerUserId)));
        cacheManager.invalidateObject(true, User.class, playerUserId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, user);
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private PlayerView toPlayerView(
            Player player, Map<Long, User> usersById, Map<Long, Device> devicesById) {
        PlayerView view = new PlayerView();
        view.setPlayerId(player.getId());
        view.setName(player.getName());
        view.setUserId(player.getUserId());
        view.setDeviceId(player.getDeviceId());
        view.setActive(player.getActive());

        User user = usersById.get(player.getUserId());
        if (user != null) {
            view.setUserLogin(user.getLogin());
            view.setUserDisplayName(user.getName());
        }

        Device device = devicesById.get(player.getDeviceId());
        if (device != null) {
            view.setDeviceName(device.getName());
            view.setDeviceUniqueId(device.getUniqueId());
            view.setClientSetupLink(setupClientService.buildSetupLink(device.getUniqueId()));
        }
        return view;
    }

}
