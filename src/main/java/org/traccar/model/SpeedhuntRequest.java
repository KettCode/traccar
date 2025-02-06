package org.traccar.model;


import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_speedhuntRequests")
public class SpeedhuntRequest extends BaseModel {
    private long speedhuntsid;
    public long getSpeedhuntsid() { return speedhuntsid; }
    public void setSpeedhuntsid(long speedhuntsid) { this.speedhuntsid = speedhuntsid; }

    private Date time;
    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }

    private long userId;
    public long getUserId() {
        return userId;
    }
    public void setUserId(long userId) {
        this.userId = userId;
    }

    private long pos;
    public long getPos() { return pos; }
    public void setPos(long pos) { this.pos = pos; }
}
