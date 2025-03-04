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

    private List<Group> groups;
    public List<Group> getGroups() {
        return groups;
    }
    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    private List<Device> devices;
    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    private List<Device> huntedDevices;
    public List<Device> getHuntedDevices() {
        return huntedDevices;
    }
    public void setHuntedDevices(List<Device> huntedDevices) {
        this.huntedDevices = huntedDevices;
    }

    private boolean isManhuntRunning;
    public boolean getIsManhuntRunning() {
        return getManhunt() != null;
    }
}
