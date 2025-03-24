package org.traccar.model;


import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_speedHuntRequests")
public class SpeedHuntRequest extends BaseModel {
    private long speedHuntsId;
    public long getSpeedHuntsId() { return speedHuntsId; }
    public void setSpeedHuntsId(long speedHuntsId) { this.speedHuntsId = speedHuntsId; }

    private long userId;
    public long getUserId() {
        return userId;
    }
    public void setUserId(long userId) {
        this.userId = userId;
    }

    private Date time;
    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }
}
