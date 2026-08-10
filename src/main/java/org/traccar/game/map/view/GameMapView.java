package org.traccar.game.map.view;

import org.traccar.game.map.GameMapGeofence;
import org.traccar.game.map.GameMapMarker;
import org.traccar.game.map.GameMapRevealMarker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameMapView {

    private long gameId;
    private List<GameMapMarker> memberMarkers = new ArrayList<>();
    private List<GameMapGeofence> geofences = new ArrayList<>();
    private List<GameMapRevealMarker> revealedMarkers = new ArrayList<>();
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

    public List<GameMapRevealMarker> getRevealedMarkers() {
        return revealedMarkers;
    }

    public void setRevealedMarkers(List<GameMapRevealMarker> revealedMarkers) {
        this.revealedMarkers = revealedMarkers;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
