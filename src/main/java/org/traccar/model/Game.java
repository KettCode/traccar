package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_games")
public class Game extends BaseModel {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_FINISHED = "finished";

    private String name;
    private String status = STATUS_DRAFT;
    private int pingIntervalSeconds = 900;
    private int speedhuntLimit;
    private int speedhuntPingLimit = 3;
    private boolean allowConsecutiveSpeedhuntsSameTarget;
    private int fakePingMaxDistanceMeters = 1000;
    private boolean locationReminderEnabled = true;
    private int maxPositionAgeSeconds = 300;
    private int locationReminderIntervalSeconds = 300;
    private Date plannedEndAt;
    private Date startedAt;
    private Date finishedAt;
    private Date createdAt = new Date();
    private Date updatedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPingIntervalSeconds() {
        return pingIntervalSeconds;
    }

    public void setPingIntervalSeconds(int pingIntervalSeconds) {
        this.pingIntervalSeconds = pingIntervalSeconds;
    }

    public int getSpeedhuntLimit() {
        return speedhuntLimit;
    }

    public void setSpeedhuntLimit(int speedhuntLimit) {
        this.speedhuntLimit = speedhuntLimit;
    }

    public int getSpeedhuntPingLimit() {
        return speedhuntPingLimit;
    }

    public void setSpeedhuntPingLimit(int speedhuntPingLimit) {
        this.speedhuntPingLimit = speedhuntPingLimit;
    }

    public boolean getAllowConsecutiveSpeedhuntsSameTarget() {
        return allowConsecutiveSpeedhuntsSameTarget;
    }

    public void setAllowConsecutiveSpeedhuntsSameTarget(boolean allowConsecutiveSpeedhuntsSameTarget) {
        this.allowConsecutiveSpeedhuntsSameTarget = allowConsecutiveSpeedhuntsSameTarget;
    }

    public int getFakePingMaxDistanceMeters() {
        return fakePingMaxDistanceMeters;
    }

    public void setFakePingMaxDistanceMeters(int fakePingMaxDistanceMeters) {
        this.fakePingMaxDistanceMeters = fakePingMaxDistanceMeters;
    }

    public boolean getLocationReminderEnabled() {
        return locationReminderEnabled;
    }

    public void setLocationReminderEnabled(boolean locationReminderEnabled) {
        this.locationReminderEnabled = locationReminderEnabled;
    }

    public int getMaxPositionAgeSeconds() {
        return maxPositionAgeSeconds;
    }

    public void setMaxPositionAgeSeconds(int maxPositionAgeSeconds) {
        this.maxPositionAgeSeconds = maxPositionAgeSeconds;
    }

    public int getLocationReminderIntervalSeconds() {
        return locationReminderIntervalSeconds;
    }

    public void setLocationReminderIntervalSeconds(int locationReminderIntervalSeconds) {
        this.locationReminderIntervalSeconds = locationReminderIntervalSeconds;
    }

    public Date getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(Date plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}
