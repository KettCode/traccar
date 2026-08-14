package org.traccar.game.setup.wizard.view;

import org.traccar.game.view.GameLookupOption;
import org.traccar.model.Game;

import java.util.List;

public class WizardState {

    private Game game;
    private List<WizardMemberView> members;
    private List<WizardGeofenceView> geofences;
    private List<GameLookupOption> availableRoles;
    private List<GameLookupOption> availableGeofenceTypes;
    private List<String> issues;
    private boolean ready;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<WizardMemberView> getMembers() {
        return members;
    }

    public void setMembers(List<WizardMemberView> members) {
        this.members = members;
    }

    public List<WizardGeofenceView> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<WizardGeofenceView> geofences) {
        this.geofences = geofences;
    }

    public List<GameLookupOption> getAvailableRoles() {
        return availableRoles;
    }

    public void setAvailableRoles(List<GameLookupOption> availableRoles) {
        this.availableRoles = availableRoles;
    }

    public List<GameLookupOption> getAvailableGeofenceTypes() {
        return availableGeofenceTypes;
    }

    public void setAvailableGeofenceTypes(List<GameLookupOption> availableGeofenceTypes) {
        this.availableGeofenceTypes = availableGeofenceTypes;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public boolean getReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

}
