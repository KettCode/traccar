package org.traccar.game.setup.wizard.request;

public class WizardCopyRequest {

    private String name;
    private Boolean copySettings;
    private Boolean copyPlayers;
    private Boolean copyGeofences;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getCopySettings() {
        return copySettings == null || copySettings;
    }

    public void setCopySettings(Boolean copySettings) {
        this.copySettings = copySettings;
    }

    public boolean getCopyPlayers() {
        return copyPlayers == null || copyPlayers;
    }

    public void setCopyPlayers(Boolean copyPlayers) {
        this.copyPlayers = copyPlayers;
    }

    public boolean getCopyGeofences() {
        return copyGeofences == null || copyGeofences;
    }

    public void setCopyGeofences(Boolean copyGeofences) {
        this.copyGeofences = copyGeofences;
    }

}
