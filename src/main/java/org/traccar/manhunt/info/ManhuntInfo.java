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
}
