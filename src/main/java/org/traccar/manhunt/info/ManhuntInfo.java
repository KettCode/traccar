package org.traccar.manhunt.info;

import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.model.Manhunt;

import java.time.Duration;
import java.time.Instant;
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

    private List<DeviceDto> huntedDevices;
    public List<DeviceDto> getHuntedDevices() {
        return huntedDevices;
    }
    public void setHuntedDevices(List<DeviceDto> huntedDevices) {
        this.huntedDevices = huntedDevices;
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
