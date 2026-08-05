package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_speedhunts")
public class GameSpeedhunt extends GameBaseModel {

    private int sequenceNumber;
    private long targetMemberId;
    private long createdByUserId;
    private int maxPings;
    private Date startedAt = new Date();
    private Date endedAt;

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTargetMemberId() {
        return targetMemberId;
    }

    public void setTargetMemberId(long targetMemberId) {
        this.targetMemberId = targetMemberId;
    }

    public long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public int getMaxPings() {
        return maxPings;
    }

    public void setMaxPings(int maxPings) {
        this.maxPings = maxPings;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

}
