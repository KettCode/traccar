package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_jokers")
public class Joker extends BaseModel {
    private long manhuntsId;
    public long getManhuntsId() { return manhuntsId; }
    public void setManhuntsId(long manhuntsId) { this.manhuntsId = manhuntsId; }

    private long userId;
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    private long jokerTypeId;
    public long getJokerTypeId() { return jokerTypeId; }
    public void setJokerTypeId(long jokerTypeId) { this.jokerTypeId = jokerTypeId; }

    private int status; // 1 = AVAILABLE, 2 = USED
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    private Date unlockedAt;
    public Date getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Date unlockedAt) { this.unlockedAt = unlockedAt; }

    private Date usedAt;
    public Date getUsedAt() { return usedAt; }
    public void setUsedAt(Date usedAt) { this.usedAt = usedAt; }
}
