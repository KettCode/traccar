package org.traccar.game.joker.view;

import org.traccar.game.map.view.GameMapRevealMarker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameJokerRevealLocationsView {

    private long jokerId;
    private long revealId;
    private Date revealedAt;
    private List<GameMapRevealMarker> markers = new ArrayList<>();

    public long getJokerId() {
        return jokerId;
    }

    public void setJokerId(long jokerId) {
        this.jokerId = jokerId;
    }

    public long getRevealId() {
        return revealId;
    }

    public void setRevealId(long revealId) {
        this.revealId = revealId;
    }

    public Date getRevealedAt() {
        return revealedAt;
    }

    public void setRevealedAt(Date revealedAt) {
        this.revealedAt = revealedAt;
    }

    public List<GameMapRevealMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<GameMapRevealMarker> markers) {
        this.markers = markers;
    }

}
