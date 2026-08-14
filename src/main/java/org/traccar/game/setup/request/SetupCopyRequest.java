package org.traccar.game.setup.request;

public class SetupCopyRequest {

    private String name;
    private Boolean copySettings;
    private Boolean copyMembers;
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

    public boolean getCopyMembers() {
        return copyMembers == null || copyMembers;
    }

    public void setCopyMembers(Boolean copyMembers) {
        this.copyMembers = copyMembers;
    }

    public boolean getCopyGeofences() {
        return copyGeofences == null || copyGeofences;
    }

    public void setCopyGeofences(Boolean copyGeofences) {
        this.copyGeofences = copyGeofences;
    }

}
