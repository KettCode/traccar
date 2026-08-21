package org.traccar.game.map.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameMapView {

    private long gameId;
    private List<GameMapMarker> memberMarkers = new ArrayList<>();
    private List<GameMapGeofence> geofences = new ArrayList<>();
    private Date createdAt = new Date();

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public List<GameMapMarker> getMemberMarkers() {
        return memberMarkers;
    }

    public void setMemberMarkers(List<GameMapMarker> memberMarkers) {
        this.memberMarkers = memberMarkers;
    }

    public List<GameMapGeofence> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<GameMapGeofence> geofences) {
        this.geofences = geofences;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
