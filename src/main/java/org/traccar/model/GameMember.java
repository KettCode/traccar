package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_members")
public class GameMember extends GameBaseModel {

    public static final String ROLE_HUNTER = "hunter";
    public static final String ROLE_HUNTED = "hunted";
    public static final String ROLE_GAME_MANAGEMENT = "game_management";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CAUGHT = "caught";
    public static final String STATUS_LEFT = "left";

    private long playerId;
    private String role;
    private String status = STATUS_ACTIVE;
    private String displayName;
    private boolean canStartSpeedhunt;
    private boolean canRequestSpeedhuntPing;
    private Date caughtAt;
    private long lastVisiblePingId;
    private Date lastLocationReminderAt;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean getCanStartSpeedhunt() {
        return canStartSpeedhunt;
    }

    public void setCanStartSpeedhunt(boolean canStartSpeedhunt) {
        this.canStartSpeedhunt = canStartSpeedhunt;
    }

    public boolean getCanRequestSpeedhuntPing() {
        return canRequestSpeedhuntPing;
    }

    public void setCanRequestSpeedhuntPing(boolean canRequestSpeedhuntPing) {
        this.canRequestSpeedhuntPing = canRequestSpeedhuntPing;
    }

    public Date getCaughtAt() {
        return caughtAt;
    }

    public void setCaughtAt(Date caughtAt) {
        this.caughtAt = caughtAt;
    }

    public long getLastVisiblePingId() {
        return lastVisiblePingId;
    }

    public void setLastVisiblePingId(long lastVisiblePingId) {
        this.lastVisiblePingId = lastVisiblePingId;
    }

    public Date getLastLocationReminderAt() {
        return lastLocationReminderAt;
    }

    public void setLastLocationReminderAt(Date lastLocationReminderAt) {
        this.lastLocationReminderAt = lastLocationReminderAt;
    }

}
