package org.traccar.manhunt.info;
import org.traccar.manhunt.dto.SpeedHuntDto;
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

    private List<SpeedHuntDto> speedHunts;
    public List<SpeedHuntDto> getSpeedHunts() {
        return speedHunts;
    }
    public void setSpeedHunts(List<SpeedHuntDto> speedHunts) {
        this.speedHunts = speedHunts;
    }

    private SpeedHuntDto lastSpeedHunt;
    public SpeedHuntDto getLastSpeedHunt() {
        var speedHunts = getSpeedHunts();
        if(speedHunts.isEmpty())
            return null;

        speedHunts.sort(Comparator.comparing(SpeedHunt::getId));
        return speedHunts.get(speedHunts.size() - 1);
    }
}
