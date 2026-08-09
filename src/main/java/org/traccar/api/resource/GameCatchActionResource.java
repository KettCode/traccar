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
import org.traccar.game.catching.GameCatchService;
import org.traccar.game.catching.request.CreateCatchRequest;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameCatchActionResource extends BaseResource {

    @Inject
    private GameCatchService catchService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/catches")
    @POST
    public Response createCatch(@PathParam("gameId") long gameId, CreateCatchRequest entity) throws Exception {
        if (entity == null || entity.getCaughtMemberId() == 0) {
            throw new IllegalArgumentException("Caught member is required");
        }
        var catchItem = catchService.createCatch(getUserId(), gameId, entity.getCaughtMemberId(), entity.getNote(), request);
        if (catchItem == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(catchItem).build();
    }

    @Path("{gameId}/catches/{catchId}/revert")
    @POST
    public Response revertCatch(@PathParam("gameId") long gameId, @PathParam("catchId") long catchId)
            throws Exception {
        var catchItem = catchService.revertCatch(getUserId(), gameId, catchId, request);
        if (catchItem == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(catchItem).build();
    }

}
