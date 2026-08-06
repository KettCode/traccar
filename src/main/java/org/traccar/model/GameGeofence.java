package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_geofences")
public class GameGeofence extends GameBaseModel {

    public static final String TYPE_PLAYFIELD = "playfield";
    public static final String TYPE_SAFE_ZONE = "safe_zone";
    public static final String TYPE_RESTRICTED_ZONE = "restricted_zone";
    public static final String TYPE_EVENT_ZONE = "event_zone";

    private long geofenceId;
    private String name;
    private String type = TYPE_PLAYFIELD;
    private String role;
    private boolean active = true;
    private Date createdAt = new Date();
    private Date updatedAt;

    public long getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(long geofenceId) {
        this.geofenceId = geofenceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}
