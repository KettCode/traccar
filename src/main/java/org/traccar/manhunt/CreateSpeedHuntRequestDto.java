package org.traccar.manhunt;

public class CreateSpeedHuntRequestDto implements IContainsManhunt, IContainsDevice, IContainsSpeedHunt {
    private long manhuntId;

    @Override
    public long getManhuntId() { return manhuntId; }

    @Override
    public void setManhuntId(long manhuntId) { this.manhuntId = manhuntId; }

    private long deviceId;

    @Override
    public long getDeviceId() {
        return deviceId;
    }

    @Override
    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private String deviceName;

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private long deviceManhuntRole;

    @Override
    public long getDeviceManhuntRole() {
        return deviceManhuntRole;
    }

    @Override
    public void setDeviceManhuntRole(long deviceManhuntRole) {
        this.deviceManhuntRole = deviceManhuntRole;
    }

    private boolean deviceIsCaught;

    @Override
    public boolean getDeviceIsCaught() {
        return deviceIsCaught;
    }

    @Override
    public void setDeviceIsCaught(boolean isCaught) {
        this.deviceIsCaught = isCaught;
    }

    private long speedHuntId;

    @Override
    public long getSpeedHuntId() {
        return speedHuntId;
    }

    @Override
    public void setSpeedHuntId(long speedHuntId) {
        this.speedHuntId = speedHuntId;
    }

    private long speedHuntRequests;

    @Override
    public long getSpeedHuntRequests() {
        return speedHuntRequests;
    }

    @Override
    public void setSpeedHuntRequests(long speedHuntRequests) {
        this.speedHuntRequests = speedHuntRequests;
    }
}
