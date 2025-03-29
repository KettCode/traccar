package org.traccar.manhunt.info;
import org.traccar.manhunt.dto.LastSpeedHuntDto;
import org.traccar.model.Manhunt;
import org.traccar.model.SpeedHunt;

import java.util.Comparator;
import java.util.List;

public class SpeedHuntInfo {
    private Manhunt manhunt;
    public Manhunt getManhunt() {
        return manhunt;
    }
    public void setManhunt(Manhunt manhunt) {
        this.manhunt = manhunt;
    }

    private List<LastSpeedHuntDto> speedHunts;
    public List<LastSpeedHuntDto> getSpeedHunts() {
        return speedHunts;
    }
    public void setSpeedHunts(List<LastSpeedHuntDto> speedHunts) {
        this.speedHunts = speedHunts;
    }

    private LastSpeedHuntDto lastSpeedHunt;
    public LastSpeedHuntDto getLastSpeedHunt() {
        var speedHunts = getSpeedHunts();
        if(speedHunts.isEmpty())
            return null;

        speedHunts.sort(Comparator.comparing(SpeedHunt::getId));
        return speedHunts.get(speedHunts.size() - 1);
    }
}
