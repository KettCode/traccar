package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.*;

@Path("locationRequests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationRequestsResource extends ExtendedObjectResource<LocationRequest> {

    public LocationRequestsResource(){
        super(LocationRequest.class, "id");
    }
}
