package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.GameBaseResource;
import org.traccar.model.GameJoker;

@Path("jokers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameJokerResource extends GameBaseResource<GameJoker> {

    public GameJokerResource() {
        super(GameJoker.class, "id");
    }

}
