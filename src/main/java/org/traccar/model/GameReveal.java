package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_reveals")
public class GameReveal extends GameBaseModel {

    public static final String TYPE_HUNTER_LOCATIONS = "hunter_locations";
    public static final String TYPE_SPEEDHUNT_TARGET = "speedhunt_target";

    private long memberId;
    private long jokerId;
    private String type;
    private long speedhuntId;
    private String payload;
    private Date revealedAt = new Date();
    private Date invalidatedAt;

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public long getJokerId() {
        return jokerId;
    }

    public void setJokerId(long jokerId) {
        this.jokerId = jokerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSpeedhuntId() {
        return speedhuntId;
    }

    public void setSpeedhuntId(long speedhuntId) {
        this.speedhuntId = speedhuntId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getRevealedAt() {
        return revealedAt;
    }

    public void setRevealedAt(Date revealedAt) {
        this.revealedAt = revealedAt;
    }

    public Date getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Date invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

}
