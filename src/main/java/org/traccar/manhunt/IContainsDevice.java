package org.traccar.manhunt;

public interface IContainsDevice {
    long getDeviceId();
    void setDeviceId(long deviceId);

    String getDeviceName();
    void setDeviceName(String deviceName);

    long getDeviceManhuntRole();
    void setDeviceManhuntRole(long deviceManhuntRole);

    boolean getDeviceIsCaught();
    void setDeviceIsCaught(boolean isCaught);
}
