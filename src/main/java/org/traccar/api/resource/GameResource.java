package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Game;
import java.util.List;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource extends ExtendedObjectResource<Game> {

    public GameResource() { super(Game.class, "name", List.of("name")); }

}
