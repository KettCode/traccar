package org.traccar.model;

import java.util.Comparator;
import java.util.List;

public class ManhuntInfo {

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

    private List<Catches> catches;
    public List<Catches> getCatches() { return catches; }
    public void setCatches(List<Catches> catches) {
        this.catches = catches;
    }

    private boolean isManhuntRunning;
    public boolean getIsManhuntRunning() {
        return getManhunt() != null;
    }
}
