package org.traccar.game.catching.request;

public class CreateCatchRequest {

    private long caughtMemberId;
    private String note;

    public long getCaughtMemberId() {
        return caughtMemberId;
    }

    public void setCaughtMemberId(long caughtMemberId) {
        this.caughtMemberId = caughtMemberId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
