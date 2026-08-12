package org.traccar.game.map.message;

import org.traccar.game.map.view.GameMapGeofence;
import org.traccar.game.map.view.GameMapMarker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameMapUpdateMessage {

    public static final String TYPE_GAME_POSITION_UPDATED = "gamePositionUpdated";
    public static final String TYPE_GAME_GEOFENCE_UPDATED = "gameGeofenceUpdated";

    private long gameId;
    private String type;
    private boolean stateRefresh;
    private List<GameMapMarker> markers = new ArrayList<>();
    private List<GameMapGeofence> geofences = new ArrayList<>();
    private List<Long> removedMemberIds = new ArrayList<>();
    private List<Long> removedGeofenceIds = new ArrayList<>();
    private Date createdAt = new Date();

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getStateRefresh() {
        return stateRefresh;
    }

    public void setStateRefresh(boolean stateRefresh) {
        this.stateRefresh = stateRefresh;
    }

    public List<GameMapMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<GameMapMarker> markers) {
        this.markers = markers;
    }

    public List<GameMapGeofence> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<GameMapGeofence> geofences) {
        this.geofences = geofences;
    }

    public List<Long> getRemovedMemberIds() {
        return removedMemberIds;
    }

    public void setRemovedMemberIds(List<Long> removedMemberIds) {
        this.removedMemberIds = removedMemberIds;
    }

    public List<Long> getRemovedGeofenceIds() {
        return removedGeofenceIds;
    }

    public void setRemovedGeofenceIds(List<Long> removedGeofenceIds) {
        this.removedGeofenceIds = removedGeofenceIds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
