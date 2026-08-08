package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameService;
import org.traccar.game.setup.wizard.request.WizardCopyRequest;
import org.traccar.model.Game;

public class WizardCopyService {

    @Inject
    private GameService gameService;

    @Inject
    private WizardGameService wizardGameService;

    @Inject
    private WizardMemberService playerService;

    @Inject
    private WizardZoneService geofenceService;

    public Game copyGame(
            long userId, long sourceGameId, WizardCopyRequest request,
            HttpServletRequest httpRequest) throws Exception {
        Game source = gameService.getAccessibleGame(userId, sourceGameId);
        if (source == null) {
            return null;
        }

        String name = request != null && request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            name = source.getName() + " Copy";
        }

        Game game = wizardGameService.createCopiedDraftGame(
                userId, source, name, request == null || request.getCopySettings(), httpRequest);

        if (request == null || request.getCopyPlayers()) {
            playerService.copyActiveMembers(userId, source.getId(), game, httpRequest);
        }
        if (request == null || request.getCopyGeofences()) {
            geofenceService.copyActiveGeofences(userId, source.getId(), game, httpRequest);
        }

        return game;
    }

}
