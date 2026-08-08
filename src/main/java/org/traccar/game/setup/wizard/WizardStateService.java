package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.game.setup.wizard.view.WizardGeofence;
import org.traccar.game.setup.wizard.view.WizardMemberView;
import org.traccar.game.setup.wizard.view.WizardState;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.Geofence;
import org.traccar.model.Device;
import org.traccar.model.Player;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.List;

public class WizardStateService {

    @Inject
    private Storage storage;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private WizardClientSetupService clientSetupService;

    public WizardState getState(long userId, long gameId) throws StorageException {
        Game game = gameService.getAccessibleGame(userId, gameId);
        if (game == null) {
            return null;
        }

        var members = storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));

        var issues = validator.getIssues(game, members);

        WizardState state = new WizardState();
        state.setGame(game);
        state.setPlayers(getSetupPlayers(members));
        state.setGeofences(getSetupGeofences(gameId));
        state.setAvailableRoles(List.of(
                GameMember.ROLE_HUNTER,
                GameMember.ROLE_HUNTED,
                GameMember.ROLE_GAME_MANAGEMENT));
        state.setAvailableGeofenceTypes(List.of(
                GameGeofence.TYPE_PLAYFIELD,
                GameGeofence.TYPE_SAFE_ZONE,
                GameGeofence.TYPE_RESTRICTED_ZONE,
                GameGeofence.TYPE_EVENT_ZONE));
        state.setIssues(issues);
        state.setReady(issues.isEmpty());
        return state;
    }

    private List<WizardMemberView> getSetupPlayers(List<GameMember> members) throws StorageException {
        var result = new ArrayList<WizardMemberView>();
        for (GameMember member : members) {
            WizardMemberView view = new WizardMemberView();
            view.setMemberId(member.getId());
            view.setPlayerId(member.getPlayerId());
            view.setDisplayName(member.getDisplayName());
            view.setRole(member.getRole());
            view.setStatus(member.getStatus());

            Player player = storage.getObject(Player.class, new Request(
                    new Columns.All(), new Condition.Equals("id", member.getPlayerId())));
            if (player != null) {
                view.setUserId(player.getUserId());
                view.setDeviceId(player.getDeviceId());

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
            }

            result.add(view);
        }
        return result;
    }

    private List<WizardGeofence> getSetupGeofences(long gameId) throws StorageException {
        var result = new ArrayList<WizardGeofence>();
        var gameGeofences = storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("active", true)), new Order("id")));
        for (GameGeofence gameGeofence : gameGeofences) {
            Geofence geofence = storage.getObject(Geofence.class, new Request(
                    new Columns.All(), new Condition.Equals("id", gameGeofence.getGeofenceId())));

            WizardGeofence setupGeofence = new WizardGeofence();
            setupGeofence.setId(gameGeofence.getId());
            setupGeofence.setGeofenceId(gameGeofence.getGeofenceId());
            setupGeofence.setName(gameGeofence.getName());
            setupGeofence.setType(gameGeofence.getType());
            setupGeofence.setRole(gameGeofence.getRole());
            setupGeofence.setActive(gameGeofence.getActive());
            if (geofence != null) {
                setupGeofence.setArea(geofence.getArea());
                if (setupGeofence.getName() == null) {
                    setupGeofence.setName(geofence.getName());
                }
            }
            result.add(setupGeofence);
        }
        return result;
    }

}
