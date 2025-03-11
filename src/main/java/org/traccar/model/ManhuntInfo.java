package org.traccar.model;

import org.traccar.manhunt.DeviceInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
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

    private List<DeviceInfo> huntedDevices;
    public List<DeviceInfo> getHuntedDevices() {
        return huntedDevices;
    }
    public void setHuntedDevices(List<DeviceInfo> huntedDevices) {
        this.huntedDevices = huntedDevices;
    }

    private SpeedHunt lastSpeedHunt;
    public SpeedHunt getLastSpeedHunt() {
        var speedHunts = getSpeedHunts();
        if(speedHunts.isEmpty())
            return null;

        speedHunts.sort(Comparator.comparing(SpeedHunt::getId));
        return speedHunts.get(speedHunts.size() - 1);
    }

    private Group lastSpeedHuntGroup;
    public Group getLastSpeedHuntGroup() {
        if(getLastSpeedHunt() == null || getGroups() == null)
            return null;

        var lastSpeedHunt = getLastSpeedHunt();
        return getGroups()
                .stream().filter(x -> x.getId() == lastSpeedHunt.getHunterGroupId())
                .findFirst()
                .orElse(new Group());
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        var speedHunts = getSpeedHunts();
        if(speedHunts == null || speedHunts.isEmpty() || getGroups() == null)
            return false;

        var lastSpeedHunt = getLastSpeedHunt();
        var group = getLastSpeedHuntGroup();
        var isRunning = lastSpeedHunt.getSpeedHuntRequests().size() < group.getSpeedHuntRequests();

        var catches = getCatches();
        if(catches == null || catches.isEmpty())
            return isRunning;

        return isRunning && catches.stream().noneMatch(x -> x.getDeviceId() == lastSpeedHunt.getDeviceId());
    }

    private long availableSpeedHuntRequests;
    public long getAvailableSpeedHuntRequests() {
        if(!getIsSpeedHuntRunning())
            return 0;

        var lastSpeedHunt = getLastSpeedHunt();
        var lastSpeedHuntGroup = getLastSpeedHuntGroup();
        return lastSpeedHuntGroup.getSpeedHuntRequests() - lastSpeedHunt.getSpeedHuntRequests().size();
    }

    private boolean isManhuntRunning;
    public boolean getIsManhuntRunning() {
        return getManhunt() != null;
    }

    private Date nextPosition;
    public Date getNextPosition() {
        var manhunt = getManhunt();

        if(manhunt == null)
            return null;

        var frequency = manhunt.getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = manhunt.getStart();

        var now = new Date();

        long durationSinceStart = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
        long nextEventSeconds = durationSinceStart + (frequency - (durationSinceStart % frequency));

        return Date.from(Instant.ofEpochSecond(start.toInstant().getEpochSecond() + nextEventSeconds));
    }
}
