package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import org.traccar.game.GameLookupService;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.game.setup.SetupClientService;
import org.traccar.game.setup.SetupStorage;
import org.traccar.game.setup.wizard.view.WizardGeofenceView;
import org.traccar.game.setup.wizard.view.WizardMemberView;
import org.traccar.game.setup.wizard.view.WizardState;
import org.traccar.model.Device;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WizardStateService {

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private GameLookupService gameLookupService;

    @Inject
    private SetupClientService setupClientService;

    @Inject
    private SetupStorage setupStorage;

    public WizardState getState(long userId, long gameId) throws StorageException {
        Game game = gameService.getAccessibleGame(userId, gameId);
        if (game == null) {
            return null;
        }

        var members = setupStorage.getActiveMembers(gameId);

        var editable = Game.STATUS_DRAFT.equals(game.getStatus());
        var issues = editable ? validator.getIssues(game, members) : List.<String>of();

        WizardState state = new WizardState();
        state.setGame(game);
        state.setMembers(getSetupMembers(members));
        state.setGeofences(getSetupGeofences(gameId));
        state.setAvailableRoles(gameLookupService.getMemberRoles());
        state.setAvailableGeofenceTypes(gameLookupService.getGeofenceTypes());
        state.setIssues(issues);
        state.setReady(editable && issues.isEmpty());
        return state;
    }

    private List<WizardMemberView> getSetupMembers(List<GameMember> members) throws StorageException {
        var result = new ArrayList<WizardMemberView>();
        Map<Long, Player> playersById = setupStorage.getPlayersByMembers(members);
        List<Player> players = new ArrayList<>(playersById.values());
        Map<Long, User> usersById = setupStorage.getUsersByPlayers(players);
        Map<Long, Device> devicesById = setupStorage.getDevicesByPlayers(players);
        for (GameMember member : members) {
            WizardMemberView view = new WizardMemberView();
            view.setMemberId(member.getId());
            view.setPlayerId(member.getPlayerId());
            view.setDisplayName(member.getDisplayName());
            view.setRole(member.getRole());
            view.setStatus(member.getStatus());
            view.setCanStartSpeedhunt(member.getCanStartSpeedhunt());
            view.setCanRequestSpeedhuntPing(member.getCanRequestSpeedhuntPing());

            Player player = playersById.get(member.getPlayerId());
            if (player != null) {
                view.setUserId(player.getUserId());
                view.setDeviceId(player.getDeviceId());

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
            }

            result.add(view);
        }
        return result;
    }

    private List<WizardGeofenceView> getSetupGeofences(long gameId) throws StorageException {
        var result = new ArrayList<WizardGeofenceView>();
        var gameGeofences = setupStorage.getGameGeofences(gameId);
        Map<Long, Geofence> geofencesById = setupStorage.getGeofencesByGameGeofences(gameGeofences);
        for (GameGeofence gameGeofence : gameGeofences) {
            Geofence geofence = geofencesById.get(gameGeofence.getGeofenceId());

            WizardGeofenceView setupGeofence = new WizardGeofenceView();
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
