package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_pings")
public class GamePing extends GameBaseModel {

    public static final String SOURCE_REGULAR = "regular";
    public static final String SOURCE_SPEEDHUNT = "speedhunt";
    public static final String SOURCE_FAKE = "fake";
    public static final String SOURCE_MANUAL = "manual";

    private long targetMemberId;
    private String source;
    private boolean skipped;
    private long positionId;
    private Date fixTime;
    private double latitude;
    private double longitude;
    private double accuracy;
    private long speedhuntId;
    private int sequenceNumber;
    private long consumedJokerId;
    private Date createdAt = new Date();

    public long getTargetMemberId() {
        return targetMemberId;
    }

    public void setTargetMemberId(long targetMemberId) {
        this.targetMemberId = targetMemberId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public Date getFixTime() {
        return fixTime;
    }

    public void setFixTime(Date fixTime) {
        this.fixTime = fixTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public long getSpeedhuntId() {
        return speedhuntId;
    }

    public void setSpeedhuntId(long speedhuntId) {
        this.speedhuntId = speedhuntId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getConsumedJokerId() {
        return consumedJokerId;
    }

    public void setConsumedJokerId(long consumedJokerId) {
        this.consumedJokerId = consumedJokerId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
