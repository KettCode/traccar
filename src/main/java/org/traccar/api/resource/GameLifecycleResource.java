package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.GameLifecycleService;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameLifecycleResource extends BaseResource {

    @Inject
    private GameLifecycleService lifecycleService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/activate")
    @POST
    public Response activate(@PathParam("gameId") long gameId) throws Exception {
        var game = lifecycleService.activate(getUserId(), gameId, request);
        if (game == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(game).build();
    }

    @Path("{gameId}/finish")
    @POST
    public Response finish(@PathParam("gameId") long gameId) throws Exception {
        var game = lifecycleService.finish(getUserId(), gameId, request);
        if (game == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(game).build();
    }

}
