package org.traccar.manhunt;

public class CreateCatchDto implements IContainsManhunt, IContainsDevice{
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
}
