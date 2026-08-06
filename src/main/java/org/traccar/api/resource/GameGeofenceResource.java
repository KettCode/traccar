package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.GameBaseResource;
import org.traccar.model.GameGeofence;

@Path("gameGeofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameGeofenceResource extends GameBaseResource<GameGeofence> {

    public GameGeofenceResource() {
        super(GameGeofence.class, "id");
    }

}
