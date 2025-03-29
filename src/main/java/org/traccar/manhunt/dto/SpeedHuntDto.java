package org.traccar.manhunt.dto;

import org.traccar.model.SpeedHunt;
import org.traccar.model.LocationRequest;

import java.util.ArrayList;
import java.util.List;

public class SpeedHuntDto extends SpeedHunt {
    private List<LocationRequest> locationRequests = new ArrayList<>();
    public List<LocationRequest> getLocationRequests() {
        return locationRequests;
    }
    public void setLocationRequests(List<LocationRequest> locationRequests) {
        this.locationRequests = locationRequests;
    }
}
