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
import org.traccar.game.speedhunt.GameSpeedhuntService;
import org.traccar.game.speedhunt.request.StartSpeedhuntRequest;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameSpeedhuntActionResource extends BaseResource {

    @Inject
    private GameSpeedhuntService speedhuntService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/speedhunts")
    @POST
    public Response startSpeedhunt(@PathParam("gameId") long gameId, StartSpeedhuntRequest entity) throws Exception {
        if (entity == null || entity.getTargetMemberId() == 0) {
            throw new IllegalArgumentException("Speedhunt target is required");
        }
        var speedhunt = speedhuntService.startSpeedhunt(getUserId(), gameId, entity.getTargetMemberId(), request);
        if (speedhunt == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(speedhunt).build();
    }

    @Path("{gameId}/speedhunts/{speedhuntId}/pings")
    @POST
    public Response requestSpeedhuntPing(
            @PathParam("gameId") long gameId, @PathParam("speedhuntId") long speedhuntId) throws Exception {
        var ping = speedhuntService.requestSpeedhuntPing(getUserId(), gameId, speedhuntId, request);
        if (ping == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ping).build();
    }

    @Path("{gameId}/speedhunts/{speedhuntId}/finish")
    @POST
    public Response finishSpeedhunt(
            @PathParam("gameId") long gameId, @PathParam("speedhuntId") long speedhuntId) throws Exception {
        var speedhunt = speedhuntService.finishSpeedhunt(getUserId(), gameId, speedhuntId, request);
        if (speedhunt == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(speedhunt).build();
    }

}
