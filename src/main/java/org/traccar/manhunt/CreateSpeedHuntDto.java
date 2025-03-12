package org.traccar.manhunt;

public class CreateSpeedHuntDto implements IContainsManhunt, IContainsDevice, IContainsLastSpeedHunt {
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

    private long lastSpeedHuntId;

    @Override
    public long getLastSpeedHuntId() {
        return lastSpeedHuntId;
    }

    @Override
    public void setLastSpeedHuntId(long lastSpeedHuntId) {
        this.lastSpeedHuntId = lastSpeedHuntId;
    }

    private long lastDeviceId;

    @Override
    public long getLastDeviceId() {
        return lastDeviceId;
    }

    @Override
    public void setLastDeviceId(long lastDeviceId) {
        this.lastDeviceId = lastDeviceId;
    }

    private boolean lastDeviceIsCaught;

    @Override
    public boolean getLastDeviceIsCaught() {
        return lastDeviceIsCaught;
    }

    @Override
    public void setLastDeviceIsCaught(boolean isCaught) {
        this.lastDeviceIsCaught = isCaught;
    }

    private long lastSpeedHuntRequests;

    @Override
    public long getLastSpeedHuntRequests() {
        return lastSpeedHuntRequests;
    }

    @Override
    public void setLastSpeedHuntRequests(long lastSpeedHuntRequests) {
        this.lastSpeedHuntRequests = lastSpeedHuntRequests;
    }

    private long speedHunts;

    public long getSpeedHunts() {
        return speedHunts;
    }

    public void setSpeedHunts(long speedHunts) {
        this.speedHunts = speedHunts;
    }
}
