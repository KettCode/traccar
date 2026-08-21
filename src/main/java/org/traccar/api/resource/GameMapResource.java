package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.map.GameMapService;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameMapResource extends BaseResource {

    @Inject
    private GameMapService mapService;

    @Path("{gameId}/map")
    @GET
    public Response getMap(@PathParam("gameId") long gameId) throws Exception {
        var map = mapService.getMap(getUserId(), gameId);
        if (map == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(map).build();
    }

}
