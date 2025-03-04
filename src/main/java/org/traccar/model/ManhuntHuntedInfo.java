package org.traccar.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class ManhuntHuntedInfo extends ManhuntInfo {
    private Date lastPosition;
    public Date getLastPosition() {
        var manhunt = getManhunt();
        var group = getGroup();

        if(manhunt == null | group == null)
            return null;

        var frequency = group.getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = manhunt.getStart();

        var now = new Date();

        long durationSinceStart = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
        long lastEventSeconds = durationSinceStart - (durationSinceStart % frequency);

        return Date.from(Instant.ofEpochSecond(start.toInstant().getEpochSecond() + lastEventSeconds));
    }

    private Date nextPosition;
    public Date getNextPosition() {
        var manhunt = getManhunt();
        var group = getGroup();

        if(manhunt == null | group == null)
            return null;

        var frequency = group.getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = manhunt.getStart();

        var now = new Date();

        long durationSinceStart = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
        long nextEventSeconds = durationSinceStart + (frequency - (durationSinceStart % frequency));

        return Date.from(Instant.ofEpochSecond(start.toInstant().getEpochSecond() + nextEventSeconds));
    }
}
