package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Manhunt;

@Path("manhunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManhuntResource extends ExtendedObjectResource<Manhunt> {

    public ManhuntResource() {
        super(Manhunt.class, "groupId");
    }
}
