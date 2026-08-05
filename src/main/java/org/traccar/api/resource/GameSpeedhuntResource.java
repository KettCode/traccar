package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.GameBaseResource;
import org.traccar.model.GameSpeedhunt;

@Path("speedhunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameSpeedhuntResource extends GameBaseResource<GameSpeedhunt> {

    public GameSpeedhuntResource() { super(GameSpeedhunt.class, "sequenceNumber"); }

}
