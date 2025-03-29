package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.BaseObjectResource;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.*;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.LinkedList;

@Path("speedHunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpeedHuntResource extends ExtendedObjectResource<SpeedHunt> {

    public SpeedHuntResource() {
        super(SpeedHunt.class, "id");
    }
}
