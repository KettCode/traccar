package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_manhunts")
public class Manhunt extends ExtendedModel {
    private long groupId;
    public long getGroupId() {
        return groupId;
    }
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    private Date start;
    public Date getStart() {
        return start;
    }
    public void setStart(Date start) {
        this.start = start;
    }

    private long frequency;
    public long getFrequency() {
        return frequency;
    }
    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    private long speedHuntLimit;
    public long getSpeedHuntLimit() { return speedHuntLimit; }
    public void setSpeedHuntLimit(long speedHuntLimit) { this.speedHuntLimit = speedHuntLimit; }

    private long speedHuntRequests;
    public long getSpeedHuntRequests() { return speedHuntRequests; }
    public void setSpeedHuntRequests(long speedHuntRequests) { this.speedHuntRequests = speedHuntRequests; }
}
