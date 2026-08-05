package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_jokers")
public class GameJoker extends GameBaseModel {

    public static final String TYPE_SKIP_PING = "skip_ping";
    public static final String TYPE_REQUEST_HUNTER_LOCATIONS = "request_hunter_locations";
    public static final String TYPE_REVEAL_SPEEDHUNT = "reveal_speedhunt";
    public static final String TYPE_FAKE_PING = "fake_ping";

    public static final String STATUS_UNLOCKED = "unlocked";
    public static final String STATUS_USED = "used";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_EXPIRED = "expired";

    private long memberId;
    private String type;
    private String status = STATUS_UNLOCKED;
    private Date unlockedAt = new Date();
    private long unlockedByUserId;
    private Date usedAt;
    private Date cancelledAt;

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Date unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public long getUnlockedByUserId() {
        return unlockedByUserId;
    }

    public void setUnlockedByUserId(long unlockedByUserId) {
        this.unlockedByUserId = unlockedByUserId;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

}
