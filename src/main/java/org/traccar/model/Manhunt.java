package org.traccar.model;

import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.util.Date;
import java.util.List;

@StorageName("tc_manhunts")
public class Manhunt extends ExtendedModel {
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

    private long speedHunts;
    public long getSpeedHunts() { return speedHunts; }
    public void setSpeedHunts(long speedHunts) { this.speedHunts = speedHunts; }

    private long locationRequests;
    public long getLocationRequests() { return locationRequests; }
    public void setLocationRequests(long locationRequests) { this.locationRequests = locationRequests; }
}
