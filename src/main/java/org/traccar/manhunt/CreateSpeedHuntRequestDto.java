package org.traccar.manhunt;

public class CreateSpeedHuntRequestDto {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private long manhuntRole;

    public long getManhuntRole() {
        return manhuntRole;
    }

    public void setManhuntRole(long manhuntRole) {
        this.manhuntRole = manhuntRole;
    }

    private long speedHuntRequests;

    public long getSpeedHuntRequests() {
        return speedHuntRequests;
    }

    public void setSpeedHuntRequests(long speedHuntRequests) {
        this.speedHuntRequests = speedHuntRequests;
    }

    private boolean isCaught;

    public boolean getIsCaught() {
        return isCaught;
    }

    public void setIsCaught(boolean isCaught) {
        this.isCaught = isCaught;
    }
}
