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

    private List<SpeedHunt> speedHunts;
    @QueryIgnore
    public List<SpeedHunt> getSpeedHunts() {
        return speedHunts;
    }
    @QueryIgnore
    public void setSpeedHunts(List<SpeedHunt> speedHunts) {
        this.speedHunts = speedHunts;
    }

    private List<Catches> catches;
    @QueryIgnore
    public List<Catches> getCatches() { return catches; }
    @QueryIgnore
    public void setCatches(List<Catches> catches) {
        this.catches = catches;
    }
}
