package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.setup.wizard.request.WizardCopyRequest;
import org.traccar.game.setup.wizard.request.WizardMemberRequest;
import org.traccar.game.setup.wizard.request.WizardPasswordRequest;
import org.traccar.game.setup.wizard.view.WizardReusablePlayer;
import org.traccar.game.setup.wizard.view.WizardState;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.storage.StorageException;

import java.util.List;

public class WizardService {

    @Inject
    private WizardGameService gameService;

    @Inject
    private WizardCopyService copyService;

    @Inject
    private WizardMemberService playerService;

    @Inject
    private WizardZoneService geofenceService;

    @Inject
    private WizardStateService stateService;

    public WizardState createGame(long userId, Game entity, HttpServletRequest request) throws Exception {
        Game game = gameService.createDraftGame(userId, entity, request);
        return stateService.getState(userId, game.getId());
    }

    public WizardState copyGame(long userId, long sourceGameId, WizardCopyRequest entity, HttpServletRequest request) throws Exception {
        Game game = copyService.copyGame(userId, sourceGameId, entity, request);
        if (game == null) {
            return null;
        }
        return stateService.getState(userId, game.getId());
    }

    public WizardState updateSettings(long userId, long gameId, Game settings, HttpServletRequest request) throws Exception {
        if (gameService.updateSettings(userId, gameId, settings, request) == null) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public boolean removeGame(long userId, long gameId, HttpServletRequest request) throws Exception {
        return gameService.removeGame(userId, gameId, request);
    }

    public WizardState addPlayers(
            long userId, long gameId, List<WizardMemberRequest> players,
            HttpServletRequest request) throws Exception {
        if (!playerService.addPlayers(userId, gameId, players, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public List<WizardReusablePlayer> getReusablePlayers(long userId, long gameId) throws StorageException {
        return playerService.getReusablePlayers(userId, gameId);
    }

    public WizardState reusePlayers(long userId, long gameId, List<GameMember> players, HttpServletRequest request) throws Exception {
        if (!playerService.reusePlayers(userId, gameId, players, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState updatePlayer(
            long userId, long gameId, long memberId, GameMember player,
            HttpServletRequest request) throws Exception {
        if (!playerService.updatePlayer(userId, gameId, memberId, player, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState removePlayer(
            long userId, long gameId, long memberId, HttpServletRequest request) throws Exception {
        if (!playerService.removePlayer(userId, gameId, memberId, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState updatePassword(
            long userId, long gameId, long memberId, WizardPasswordRequest password,
            HttpServletRequest request) throws Exception {
        if (!playerService.updatePassword(userId, gameId, memberId, password, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState addGeofences(
            long userId, long gameId, List<GameGeofence> geofences,
            HttpServletRequest request) throws Exception {
        if (!geofenceService.addGeofences(userId, gameId, geofences, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState updateGeofence(
            long userId, long gameId, long gameGeofenceId, GameGeofence geofence,
            HttpServletRequest request) throws Exception {
        if (!geofenceService.updateGeofence(userId, gameId, gameGeofenceId, geofence, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState removeGeofence(
            long userId, long gameId, long gameGeofenceId, HttpServletRequest request) throws Exception {
        if (!geofenceService.removeGeofence(userId, gameId, gameGeofenceId, request)) {
            return null;
        }
        return stateService.getState(userId, gameId);
    }

    public WizardState getState(long userId, long gameId) throws StorageException {
        return stateService.getState(userId, gameId);
    }

}
