package org.traccar.model;

import java.util.ArrayList;
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

    private Group group;
    public Group getGroup() {
        return group;
    }
    public void setGroup(Group group) {
        this.group = group;
    }

    public void setSpeedHuntRequests(List<SpeedHuntRequest> speedHuntRequests) {
        if(speedHunts.isEmpty())
            return;

        speedHunts.forEach(x -> {
            var speedHuntRequestsInternal = speedHuntRequests.stream()
                    .filter(y -> y.getSpeedHuntsId() == x.getId())
                    .toList();
            x.setSpeedHuntRequests(speedHuntRequestsInternal);
        });
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        if(group == null || speedHunts.isEmpty())
            return false;

        return speedHunts.get(speedHunts.size() - 1).getSpeedHuntRequests().size() < group.getSpeedHuntRequests();
    }
}
