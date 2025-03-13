package org.traccar.manhunt;

import org.traccar.model.SpeedHunt;

public class SpeedHuntDto extends SpeedHunt {
    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private boolean deviceIsCaught;

    public boolean getIsDeviceIsCaught() {
        return deviceIsCaught;
    }

    public void setDeviceIsCaught(boolean deviceIsCaught) {
        this.deviceIsCaught = deviceIsCaught;
    }

    private long maxRequests;

    public long getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(long maxRequests) {
        this.maxRequests = maxRequests;
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        return !getIsDeviceIsCaught() && getAvailableSpeedHuntRequests() > 0;
    }

    private long availableSpeedHuntRequests;
    public long getAvailableSpeedHuntRequests() {
        return getMaxRequests() - getSpeedHuntRequests().size();
    }
}
