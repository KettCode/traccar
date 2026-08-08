package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.SetupPlayerService;

@Path("setup/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupPlayerResource extends BaseResource {

    @Inject
    private SetupPlayerService setupPlayerService;

    @Context
    private HttpServletRequest request;

    @GET
    public Response get(@QueryParam("includeInactive") boolean includeInactive) throws Exception {
        return Response.ok(setupPlayerService.getPlayers(getUserId(), includeInactive)).build();
    }

    @Path("{playerId}")
    @DELETE
    public Response remove(@PathParam("playerId") long playerId) throws Exception {
        if (!setupPlayerService.removePlayer(getUserId(), playerId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
