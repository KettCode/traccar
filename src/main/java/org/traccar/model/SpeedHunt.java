package org.traccar.model;

import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.util.Date;
import java.util.List;

@StorageName("tc_speedHunts")
public class SpeedHunt extends BaseModel {
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

    private long pos;
    public long getPos() {
        return pos;
    }
    public void setPos(long pos) {
        this.pos = pos;
    }

    private Date lastTime;
    public Date getLastTime() { return lastTime; }
    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }


    private List<SpeedHuntRequest> speedHuntRequests;
    @QueryIgnore
    public List<SpeedHuntRequest> getSpeedHuntRequests() {
        return speedHuntRequests;
    }
    @QueryIgnore
    public void setSpeedHuntRequests(List<SpeedHuntRequest> speedHuntRequests) {
        this.speedHuntRequests = speedHuntRequests;
    }
}
