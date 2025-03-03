package org.traccar.model;

import java.util.Comparator;

public class ManhuntHunterInfo extends ManhuntInfo {

    public SpeedHunt getLastSpeedHunt() {
        var speedHunts = getSpeedHunts();
        if(speedHunts.isEmpty())
            return null;

        speedHunts.sort(Comparator.comparing(SpeedHunt::getId));
        return speedHunts.get(speedHunts.size() - 1);
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        var group = getGroup();
        var speedHunts = getSpeedHunts();
        if(group == null || speedHunts.isEmpty())
            return false;

        var lastSpeedHunt = getLastSpeedHunt();
        var isRunning = lastSpeedHunt.getSpeedHuntRequests().size() < group.getSpeedHuntRequests();

        var catches = getCatches();
        if(catches == null || catches.isEmpty())
            return isRunning;

        return isRunning && catches.stream().noneMatch(x -> x.getDeviceId() == lastSpeedHunt.getDeviceId());
    }
}
