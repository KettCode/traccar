package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_speedhunts")
public class Speedhunt extends BaseModel {
    private long manhuntsId;
    public long getManhuntsId() {
        return manhuntsId;
    }
    public void setManhuntsId(long manhuntsId) {
        this.manhuntsId = manhuntsId;
    }

    private long deviceId;
    public long getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private long pos;
    public long getPos() {
        return pos;
    }
    public void setPos(long pos) {
        this.pos = pos;
    }

    private Date lastTime;
    public Date getLastTime() {
        return lastTime;
    }
    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }
}
