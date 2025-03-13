package org.traccar.manhunt.dto;

import org.traccar.model.SpeedHunt;

public class SpeedHuntDto extends SpeedHunt {
    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private long deviceGroupId;

    public long getDeviceGroupId() {
        return deviceGroupId;
    }

    public void setDeviceGroupId(long deviceGroupId) {
        this.deviceGroupId = deviceGroupId;
    }

    private long deviceManhuntRole;

    public long getDeviceManhuntRole() { return deviceManhuntRole; }

    public void setDeviceManhuntRole(long deviceManhuntRole) { this.deviceManhuntRole = deviceManhuntRole; }

    private boolean deviceIsCaught;

    public boolean getDeviceIsCaught() {
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

    private long numRequests;

    public long getNumRequests() {
        return numRequests;
    }

    public void setNumRequests(long numRequests) {
        this.numRequests = numRequests;
    }

    private boolean isSpeedHuntRunning;
    public boolean getIsSpeedHuntRunning() {
        return !getDeviceIsCaught() && getAvailableSpeedHuntRequests() > 0;
    }

    private long availableSpeedHuntRequests;
    public long getAvailableSpeedHuntRequests() {
        return getMaxRequests() - getSpeedHuntRequests().size();
    }
}
