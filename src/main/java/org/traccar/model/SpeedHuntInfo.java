package org.traccar.model;

import java.util.List;

public class SpeedHuntInfo {
    private Manhunt manhunt;
    public Manhunt getManhunt() {
        return manhunt;
    }
    public void setManhunt(Manhunt manhunt) {
        this.manhunt = manhunt;
    }

    private List<SpeedHunt> speedHunts;
    public List<SpeedHunt> getSpeedHunts() {
        return speedHunts;
    }
    public void setSpeedHunts(List<SpeedHunt> speedHunts) {
        this.speedHunts = speedHunts;
    }

    private List<SpeedHuntRequest> speedHuntRequests;
    public List<SpeedHuntRequest> getSpeedHuntRequests() {
        return speedHuntRequests;
    }
    public void setSpeedHuntRequests(List<SpeedHuntRequest> speedHuntRequests) {
        this.speedHuntRequests = speedHuntRequests;
    }
}
