package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.joker.GameJokerService;
import org.traccar.game.joker.request.ActivateJokerRequest;
import org.traccar.game.joker.request.UnlockJokerRequest;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameJokerActionResource extends BaseResource {

    @Inject
    private GameJokerService jokerService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/jokers/unlock")
    @POST
    public Response unlockJoker(@PathParam("gameId") long gameId, UnlockJokerRequest entity) throws Exception {
        if (entity == null || entity.getMemberId() == 0 || entity.getType() == null || entity.getType().isBlank()) {
            throw new IllegalArgumentException("Joker member and type are required");
        }
        var joker = jokerService.unlockJoker(getUserId(), gameId, entity.getMemberId(), entity.getType(), request);
        if (joker == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(joker).build();
    }

    @Path("{gameId}/jokers/{jokerId}/activate")
    @POST
    public Response activateJoker(
            @PathParam("gameId") long gameId, @PathParam("jokerId") long jokerId,
            ActivateJokerRequest entity) throws Exception {
        var joker = jokerService.activateJoker(getUserId(), gameId, jokerId, entity, request);
        if (joker == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(joker).build();
    }

    @Path("{gameId}/jokers/{jokerId}/cancel")
    @POST
    public Response cancelJoker(@PathParam("gameId") long gameId, @PathParam("jokerId") long jokerId)
            throws Exception {
        var joker = jokerService.cancelJoker(getUserId(), gameId, jokerId, request);
        if (joker == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(joker).build();
    }

    @Path("{gameId}/jokers/{jokerId}/revealed-locations")
    @GET
    public Response getRevealedLocations(
            @PathParam("gameId") long gameId, @PathParam("jokerId") long jokerId) throws Exception {
        var view = jokerService.getRevealedLocations(getUserId(), gameId, jokerId);
        if (view == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(view).build();
    }

}
