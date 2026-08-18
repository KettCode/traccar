package org.traccar.game.setup.request;

public class SetupMemberRequest {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    private boolean canStartSpeedhunt;

    public boolean getCanStartSpeedhunt() {
        return canStartSpeedhunt;
    }

    public void setCanStartSpeedhunt(boolean canStartSpeedhunt) {
        this.canStartSpeedhunt = canStartSpeedhunt;
    }

    private boolean canRequestSpeedhuntPing;

    public boolean getCanRequestSpeedhuntPing() {
        return canRequestSpeedhuntPing;
    }

    public void setCanRequestSpeedhuntPing(boolean canRequestSpeedhuntPing) {
        this.canRequestSpeedhuntPing = canRequestSpeedhuntPing;
    }

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
