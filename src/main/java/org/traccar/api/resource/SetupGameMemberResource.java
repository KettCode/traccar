package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.SetupGameMemberService;
import org.traccar.game.setup.SetupPlayerService;
import org.traccar.game.setup.request.SetupMemberRequest;
import org.traccar.model.GameMember;

import java.util.List;

@Path("setup/games/{gameId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupGameMemberResource extends BaseResource {

    @Inject
    private SetupGameMemberService setupGameMemberService;

    @Inject
    private SetupPlayerService setupPlayerService;

    @Context
    private HttpServletRequest request;

    @POST
    public Response addMembers(
            @PathParam("gameId") long gameId, List<SetupMemberRequest> members) throws Exception {
        if (!setupGameMemberService.addMembers(getUserId(), gameId, members, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("availablePlayers")
    @GET
    public Response getAvailablePlayers(@PathParam("gameId") long gameId) throws Exception {
        var players = setupPlayerService.getAvailablePlayers(getUserId(), gameId);
        if (players == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(players).build();
    }

    @Path("existingPlayers")
    @POST
    public Response addExistingPlayers(
            @PathParam("gameId") long gameId, List<GameMember> members) throws Exception {
        if (!setupGameMemberService.addExistingPlayers(getUserId(), gameId, members, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{memberId}")
    @PUT
    public Response updateMember(
            @PathParam("gameId") long gameId, @PathParam("memberId") long memberId,
            GameMember member) throws Exception {
        if (!setupGameMemberService.updateMember(getUserId(), gameId, memberId, member, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{memberId}")
    @DELETE
    public Response removeMember(
            @PathParam("gameId") long gameId, @PathParam("memberId") long memberId) throws Exception {
        if (!setupGameMemberService.removeMember(getUserId(), gameId, memberId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
