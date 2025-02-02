package org.traccar.model;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_locations")
public class Location extends ExtendedModel {
    private long groupId;
    public long getGroupId() {
        return groupId;
    }
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    private long targetGroupId;
    public long getTargetGroupId() {
        return targetGroupId;
    }
    public void setTargetGroupId(long targetGroupId) {
        this.targetGroupId = targetGroupId;
    }

    private Date start;
    public Date getStart() {
        return start;
    }
    public void setStart(Date start) {
        this.start = start;
    }

    private long frequency;
    public long getFrequency() {
        return frequency;
    }
    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }
}
