package org.traccar.manhunt.dto;

import org.traccar.model.Manhunt;
import org.traccar.model.SpeedHunt;

import java.util.ArrayList;
import java.util.List;

public class ManhuntDto extends Manhunt {
    private List<DeviceDto> devices = new ArrayList<>();
    public List<DeviceDto> getDevices() {
        return devices;
    }
    public void setDevices(List<DeviceDto> devices) { this.devices = devices; }

    private List<DeviceDto> hunterDevices;
    public List<DeviceDto> getHunterDevices() {
        return devices
                .stream().filter(x -> x.getManhuntRole() == 1)
                .toList();
    }

    private List<DeviceDto> huntedDevices;
    public List<DeviceDto> getHuntedDevices() {
        return devices
                .stream().filter(x -> x.getManhuntRole() == 2)
                .toList();
    }

    private SpeedHuntDto lastSpeedHunt = new SpeedHuntDto();
    public SpeedHuntDto getLastSpeedHunt() { return this.lastSpeedHunt; }
    public void setLastSpeedHunt(SpeedHuntDto speedHunt) { this.lastSpeedHunt = speedHunt; }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        return lastSpeedHunt.getLocationRequests().size() < getLocationRequests()
                && devices.stream().anyMatch(x -> x.getId() == lastSpeedHunt.getDeviceId() && !x.getIsCaught());
    }

    private long availableLocationRequests;
    public long getAvailableLocationRequests() {
        return getLocationRequests() - lastSpeedHunt.getLocationRequests().size();
    }
}
