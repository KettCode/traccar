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
import org.traccar.game.member.GameMemberActionService;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameMemberActionResource extends BaseResource {

    @Inject
    private GameMemberActionService memberActionService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/members/{memberId}/convertToHunter")
    @POST
    public Response convertToHunter(@PathParam("gameId") long gameId, @PathParam("memberId") long memberId)
            throws Exception {
        var member = memberActionService.convertCaughtHuntedToHunter(getUserId(), gameId, memberId, request);
        if (member == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(member).build();
    }

}
