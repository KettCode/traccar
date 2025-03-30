package org.traccar.model;

import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@StorageName("tc_manhunts")
public class Manhunt extends BaseModel {
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

    private long locationRequestLimit;
    public long getLocationRequestLimit() { return locationRequestLimit; }
    public void setLocationRequestLimit(long locationRequestLimit) { this.locationRequestLimit = locationRequestLimit; }

    private List<SpeedHunt> speedHunts = new ArrayList<>();
    @QueryIgnore
    public List<SpeedHunt> getSpeedHunts() {
        return speedHunts;
    }
    @QueryIgnore
    public void setSpeedHunts(List<SpeedHunt> speedHunts) {
        this.speedHunts = speedHunts;
    }

    private Date nextLocationReport;
    @QueryIgnore
    public Date getNextLocationReport() {
        var frequency = getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = getStart();

        var now = new Date();
        if(now.before(start))
            return start;

        long durationSinceStart = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
        long nextEventSeconds = durationSinceStart + (frequency - (durationSinceStart % frequency));

        return Date.from(Instant.ofEpochSecond(start.toInstant().getEpochSecond() + nextEventSeconds));
    }

    private long nextLocationReportSeconds;
    @QueryIgnore
    public long getNextLocationReportSeconds() {
        var frequency = getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = getStart();

        var now = new Date();
        if(now.before(start)){
            return Duration.between(now.toInstant(), start.toInstant()).getSeconds();
        }

        var durationBetween = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
        var remainder = durationBetween % frequency;
        return remainder == 0 ? 0 : (frequency - remainder);
    }
}
