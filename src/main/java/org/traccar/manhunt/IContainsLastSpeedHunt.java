package org.traccar.manhunt;

public interface IContainsLastSpeedHunt {
    long getLastSpeedHuntId();
    void setLastSpeedHuntId(long speedHuntId);

    long getLastDeviceId();
    void setLastDeviceId(long deviceId);

    boolean getLastDeviceIsCaught();
    void setLastDeviceIsCaught(boolean isCaught);

    long getLastSpeedHuntRequests();
    void setLastSpeedHuntRequests(long speedHuntRequests);
}
