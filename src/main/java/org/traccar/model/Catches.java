package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_catches")
public class Catches extends BaseModel {
    private long manhuntsId;
    public long getManhuntsId() {
        return manhuntsId;
    }
    public void setManhuntsId(long manhuntsId) {
        this.manhuntsId = manhuntsId;
    }

    private long hunterGroupId;
    public long getHunterGroupId() { return hunterGroupId; }
    public void setHunterGroupId(long hunterGroupId) { this.hunterGroupId = hunterGroupId; }

    private long deviceId;
    public long getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private Date time;
    public Date getTime() { return time; }
    public void setTime(Date time) {
        this.time = time;
    }
}
