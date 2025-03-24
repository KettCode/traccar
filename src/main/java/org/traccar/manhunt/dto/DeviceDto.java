package org.traccar.manhunt.dto;

import org.traccar.model.Device;

public class DeviceDto extends Device {
    private boolean isCaught;

    public boolean getIsCaught() {
        return isCaught;
    }

    public void setIsCaught(boolean isCaught) {
        this.isCaught = isCaught;
    }
}
