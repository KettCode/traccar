package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.game.setup.wizard.WizardClientSetupService;
import org.traccar.game.setup.view.SetupPlayerView;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.GameMember;
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
    private WizardClientSetupService clientSetupService;

    public List<SetupPlayerView> getPlayers(long userId, boolean includeInactive) throws StorageException {
        var conditions = new ArrayList<Condition>();
        if (!includeInactive) {
            conditions.add(new Condition.Equals("active", true));
        }
        if (permissionsService.notAdmin(userId)) {
            conditions.add(new Condition.Permission(User.class, userId, Player.class));
        }
        var result = new ArrayList<SetupPlayerView>();
        var players = storage.getObjects(Player.class, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("id")));
        for (Player player : players) {
            result.add(toPlayerView(player));
        }
        return result;
    }

    public boolean removePlayer(long userId, long playerId, HttpServletRequest request) throws Exception {
        permissionsService.checkPermission(Player.class, userId, playerId);
        permissionsService.checkEdit(userId, Player.class, false, false);

        Player player = storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
        if (player == null) {
            return false;
        }

        if (isReferenced(playerId)) {
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

    private boolean isReferenced(long playerId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.Include("id"), new Condition.Equals("playerId", playerId))) != null;
    }

    private SetupPlayerView toPlayerView(Player player) throws StorageException {
        SetupPlayerView view = new SetupPlayerView();
        view.setPlayerId(player.getId());
        view.setUserId(player.getUserId());
        view.setDeviceId(player.getDeviceId());
        view.setActive(player.getActive());

        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", player.getUserId())));
        if (user != null) {
            view.setUsername(user.getLogin());
            view.setUserName(user.getName());
        }

        Device device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", player.getDeviceId())));
        if (device != null) {
            view.setDeviceName(device.getName());
            view.setDeviceUniqueId(device.getUniqueId());
            view.setClientSetupLink(clientSetupService.buildSetupLink(device.getUniqueId()));
        }
        return view;
    }

}
