package org.traccar.manhunt.dto;

import org.traccar.model.Device;

public class DeviceDto extends Device {
    private long manhuntRole;

    public long getManhuntRole() {
        return manhuntRole;
    }

    public void setManhuntRole(long manhuntRole) {
        this.manhuntRole = manhuntRole;
    }

    private boolean isCaught;

    public boolean getIsCaught() {
        return isCaught;
    }

    public void setIsCaught(boolean isCaught) {
        this.isCaught = isCaught;
    }
}
