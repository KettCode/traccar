package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.BaseObjectResource;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Location;

@Path("locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource extends ExtendedObjectResource<Location> {

    public LocationResource() {
        super(Location.class, "groupId");
    }
}
