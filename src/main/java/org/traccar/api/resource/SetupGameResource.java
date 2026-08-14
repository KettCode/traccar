package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.SetupGameService;
import org.traccar.game.setup.request.SetupCopyRequest;
import org.traccar.model.Game;

@Path("setup/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupGameResource extends BaseResource {

    @Inject
    private SetupGameService setupGameService;

    @Context
    private HttpServletRequest request;

    @GET
    public Response get() throws Exception {
        return Response.ok(setupGameService.getGames(getUserId())).build();
    }

    @POST
    public Response create(Game entity) throws Exception {
        return Response.ok(setupGameService.createDraftGame(getUserId(), entity, request)).build();
    }

    @Path("{sourceGameId}/copy")
    @POST
    public Response copyFrom(
            @PathParam("sourceGameId") long sourceGameId, SetupCopyRequest entity) throws Exception {
        Game game = setupGameService.copyGame(getUserId(), sourceGameId, entity, request);
        if (game == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(game).build();
    }

    @Path("{gameId}/settings")
    @PUT
    public Response updateSettings(@PathParam("gameId") long gameId, Game entity) throws Exception {
        if (setupGameService.updateSettings(getUserId(), gameId, entity, request) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{gameId}")
    @DELETE
    public Response remove(@PathParam("gameId") long gameId) throws Exception {
        if (!setupGameService.removeGame(getUserId(), gameId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
