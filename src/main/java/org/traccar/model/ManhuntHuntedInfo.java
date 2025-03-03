package org.traccar.model;

import java.util.Date;

public class ManhuntHuntedInfo extends ManhuntInfo {
    public Date getLastPosition() {
        return new Date();
    }

    public Date getNextPosition() {
        return new Date();
    }

    public boolean isHunted() {
        return false;
    }
    
    public boolean isCaught() {
        return false;
    }
}
