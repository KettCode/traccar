package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.GameBaseResource;
import org.traccar.model.GameCatch;

@Path("catches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameCatchResource extends GameBaseResource<GameCatch> {

    public GameCatchResource() { super(GameCatch.class, "id"); }

}
