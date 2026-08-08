package org.traccar.game.setup.wizard.view;

import org.traccar.model.Game;

import java.util.List;

public class WizardState {

    private Game game;
    private List<WizardMemberView> players;
    private List<WizardGeofence> geofences;
    private List<String> availableRoles;
    private List<String> availableGeofenceTypes;
    private List<String> issues;
    private boolean ready;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<WizardMemberView> getPlayers() {
        return players;
    }

    public void setPlayers(List<WizardMemberView> players) {
        this.players = players;
    }

    public List<WizardGeofence> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<WizardGeofence> geofences) {
        this.geofences = geofences;
    }

    public List<String> getAvailableRoles() {
        return availableRoles;
    }

    public void setAvailableRoles(List<String> availableRoles) {
        this.availableRoles = availableRoles;
    }

    public List<String> getAvailableGeofenceTypes() {
        return availableGeofenceTypes;
    }

    public void setAvailableGeofenceTypes(List<String> availableGeofenceTypes) {
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
