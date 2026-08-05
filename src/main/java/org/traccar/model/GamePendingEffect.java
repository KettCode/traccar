package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_pending_effects")
public class GamePendingEffect extends GameBaseModel {

    public static final String EFFECT_SKIP_NEXT_PING = "skip_next_ping";
    public static final String EFFECT_FAKE_NEXT_PING = "fake_next_ping";

    private long memberId;
    private long jokerId;
    private String effect;
    private boolean active = true;
    private String payload;
    private Date createdAt = new Date();
    private Date consumedAt;
    private long consumedPingId;

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

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Date consumedAt) {
        this.consumedAt = consumedAt;
    }

    public long getConsumedPingId() {
        return consumedPingId;
    }

    public void setConsumedPingId(long consumedPingId) {
        this.consumedPingId = consumedPingId;
    }

}
