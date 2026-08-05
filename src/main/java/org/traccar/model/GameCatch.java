package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_game_catches")
public class GameCatch extends GameBaseModel {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REVERTED = "reverted";

    private long caughtMemberId;
    private long reportedByUserId;
    private String status = STATUS_ACTIVE;
    private Date caughtAt = new Date();
    private long positionId;
    private double latitude;
    private double longitude;
    private String note;
    private Date revertedAt;
    private long revertedByUserId;

    public long getCaughtMemberId() {
        return caughtMemberId;
    }

    public void setCaughtMemberId(long caughtMemberId) {
        this.caughtMemberId = caughtMemberId;
    }

    public long getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(long reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCaughtAt() {
        return caughtAt;
    }

    public void setCaughtAt(Date caughtAt) {
        this.caughtAt = caughtAt;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getRevertedAt() {
        return revertedAt;
    }

    public void setRevertedAt(Date revertedAt) {
        this.revertedAt = revertedAt;
    }

    public long getRevertedByUserId() {
        return revertedByUserId;
    }

    public void setRevertedByUserId(long revertedByUserId) {
        this.revertedByUserId = revertedByUserId;
    }

}
