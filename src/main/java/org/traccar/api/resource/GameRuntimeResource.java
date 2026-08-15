package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.GameCurrentService;
import org.traccar.game.state.GameStateService;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameRuntimeResource extends BaseResource {

    @Inject
    private GameCurrentService currentService;

    @Inject
    private GameStateService stateService;

    @Path("current")
    @GET
    public Response getCurrent() throws Exception {
        var current = currentService.getCurrent(getUserId());
        if (current == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(current).build();
    }

    @Path("{gameId}/state")
    @GET
    public Response getState(@PathParam("gameId") long gameId, @QueryParam("include") String include)
            throws Exception {
        var state = stateService.getState(getUserId(), gameId, include);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

}
