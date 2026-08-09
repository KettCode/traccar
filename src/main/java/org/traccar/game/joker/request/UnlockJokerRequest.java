package org.traccar.game.joker.request;

public class UnlockJokerRequest {

    private long memberId;
    private String type;

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

}
