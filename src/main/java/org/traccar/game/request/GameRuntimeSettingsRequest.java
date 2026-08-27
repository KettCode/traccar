package org.traccar.game.request;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Date;

public class GameRuntimeSettingsRequest {

    private Integer pingIntervalSeconds;
    private Integer speedhuntLimit;
    private Integer speedhuntPingLimit;
    private Boolean allowConsecutiveSpeedhuntsSameTarget;
    private Boolean locationReminderEnabled;
    private Integer maxPositionAgeSeconds;
    private Integer locationReminderIntervalSeconds;
    private Date plannedEndAt;
    private boolean plannedEndAtSet;

    public Integer getPingIntervalSeconds() {
        return pingIntervalSeconds;
    }

    public void setPingIntervalSeconds(Integer pingIntervalSeconds) {
        this.pingIntervalSeconds = pingIntervalSeconds;
    }

    public Integer getSpeedhuntLimit() {
        return speedhuntLimit;
    }

    public void setSpeedhuntLimit(Integer speedhuntLimit) {
        this.speedhuntLimit = speedhuntLimit;
    }

    public Integer getSpeedhuntPingLimit() {
        return speedhuntPingLimit;
    }

    public void setSpeedhuntPingLimit(Integer speedhuntPingLimit) {
        this.speedhuntPingLimit = speedhuntPingLimit;
    }

    public Boolean getAllowConsecutiveSpeedhuntsSameTarget() {
        return allowConsecutiveSpeedhuntsSameTarget;
    }

    public void setAllowConsecutiveSpeedhuntsSameTarget(Boolean allowConsecutiveSpeedhuntsSameTarget) {
        this.allowConsecutiveSpeedhuntsSameTarget = allowConsecutiveSpeedhuntsSameTarget;
    }

    public Boolean getLocationReminderEnabled() {
        return locationReminderEnabled;
    }

    public void setLocationReminderEnabled(Boolean locationReminderEnabled) {
        this.locationReminderEnabled = locationReminderEnabled;
    }

    public Integer getMaxPositionAgeSeconds() {
        return maxPositionAgeSeconds;
    }

    public void setMaxPositionAgeSeconds(Integer maxPositionAgeSeconds) {
        this.maxPositionAgeSeconds = maxPositionAgeSeconds;
    }

    public Integer getLocationReminderIntervalSeconds() {
        return locationReminderIntervalSeconds;
    }

    public void setLocationReminderIntervalSeconds(Integer locationReminderIntervalSeconds) {
        this.locationReminderIntervalSeconds = locationReminderIntervalSeconds;
    }

    public Date getPlannedEndAt() {
        return plannedEndAt;
    }

    public boolean getPlannedEndAtSet() {
        return plannedEndAtSet;
    }

    @JsonSetter("plannedEndAt")
    public void setPlannedEndAt(Date plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
        plannedEndAtSet = true;
    }

}
