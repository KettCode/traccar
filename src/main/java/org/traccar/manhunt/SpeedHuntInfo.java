package org.traccar.manhunt;

import org.traccar.model.Device;
import org.traccar.model.Manhunt;
import org.traccar.model.SpeedHunt;

import java.util.Comparator;
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

    private SpeedHunt lastSpeedHunt;
    public SpeedHunt getLastSpeedHunt() {
        var speedHunts = getSpeedHunts();
        if(speedHunts.isEmpty())
            return null;

        speedHunts.sort(Comparator.comparing(SpeedHunt::getId));
        return speedHunts.get(speedHunts.size() - 1);
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() { return isSpeedHuntRunning; }
    public void setIsSpeedHuntRunning(boolean isRunning) {
        this.isSpeedHuntRunning = isRunning;
    }

    private long availableSpeedHuntRequests;
    public long getAvailableSpeedHuntRequests() { return availableSpeedHuntRequests; }
    public void setAvailableSpeedHuntRequests(long availableSpeedHuntRequests) { this.availableSpeedHuntRequests = availableSpeedHuntRequests; }

    private List<Device> devices;
    public List<Device> getDevices() { return devices; }
    public void setDevices(List<Device> devices) { this.devices = devices; }
}
