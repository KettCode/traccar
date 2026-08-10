package org.traccar.game.map;

import java.util.Date;

public class GameMapRevealMarker {

    private long revealId;
    private long memberId;
    private String displayName;
    private String role;
    private String status;
    private String source;
    private Long positionId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Date fixTime;
    private Date revealedAt;

    public long getRevealId() {
        return revealId;
    }

    public void setRevealId(long revealId) {
        this.revealId = revealId;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Date getFixTime() {
        return fixTime;
    }

    public void setFixTime(Date fixTime) {
        this.fixTime = fixTime;
    }

    public Date getRevealedAt() {
        return revealedAt;
    }

    public void setRevealedAt(Date revealedAt) {
        this.revealedAt = revealedAt;
    }

}
