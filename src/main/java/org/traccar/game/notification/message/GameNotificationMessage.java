package org.traccar.game.notification.message;

import java.util.Date;

public class GameNotificationMessage {

    public static final String TYPE_SPEEDHUNT_STARTED = "speedhuntStarted";
    public static final String TYPE_SPEEDHUNT_PING_CREATED = "speedhuntPingCreated";
    public static final String TYPE_SPEEDHUNT_FINISHED = "speedhuntFinished";
    public static final String TYPE_CATCH_CREATED = "catchCreated";
    public static final String TYPE_CATCH_REVERTED = "catchReverted";
    public static final String TYPE_JOKER_CHANGED = "jokerChanged";
    public static final String TYPE_REGULAR_PING_CREATED = "regularPingCreated";
    public static final String TYPE_MEMBER_CONVERTED_TO_HUNTER = "memberConvertedToHunter";
    public static final String TYPE_CURRENT_GAME_CHANGED = "currentGameChanged";
    public static final String TYPE_GAME_ACTIVATED = "gameActivated";
    public static final String TYPE_GAME_FINISHED = "gameFinished";
    public static final String TYPE_GAME_DELETED = "gameDeleted";
    public static final String TYPE_GAME_SETTINGS_CHANGED = "gameSettingsChanged";
    public static final String TYPE_MEMBER_ADDED = "memberAdded";
    public static final String TYPE_MEMBER_REMOVED = "memberRemoved";
    public static final String TYPE_MEMBER_CHANGED = "memberChanged";

    private long gameId;
    private String type;
    private boolean currentGameRefresh;
    private boolean stateRefresh;
    private Long speedhuntId;
    private Long pingId;
    private Long memberId;
    private Long jokerId;
    private Long catchId;
    private Date createdAt = new Date();

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getCurrentGameRefresh() {
        return currentGameRefresh;
    }

    public void setCurrentGameRefresh(boolean currentGameRefresh) {
        this.currentGameRefresh = currentGameRefresh;
    }

    public boolean getStateRefresh() {
        return stateRefresh;
    }

    public void setStateRefresh(boolean stateRefresh) {
        this.stateRefresh = stateRefresh;
    }

    public Long getSpeedhuntId() {
        return speedhuntId;
    }

    public void setSpeedhuntId(Long speedhuntId) {
        this.speedhuntId = speedhuntId;
    }

    public Long getPingId() {
        return pingId;
    }

    public void setPingId(Long pingId) {
        this.pingId = pingId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getJokerId() {
        return jokerId;
    }

    public void setJokerId(Long jokerId) {
        this.jokerId = jokerId;
    }

    public Long getCatchId() {
        return catchId;
    }

    public void setCatchId(Long catchId) {
        this.catchId = catchId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
