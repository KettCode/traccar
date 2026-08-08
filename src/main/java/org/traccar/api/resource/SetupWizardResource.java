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
import org.traccar.game.setup.wizard.WizardService;
import org.traccar.game.setup.wizard.request.WizardCopyRequest;
import org.traccar.game.setup.wizard.request.WizardMemberRequest;
import org.traccar.game.setup.wizard.request.WizardPasswordRequest;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;

import java.util.List;

@Path("setup/wizard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupWizardResource extends BaseResource {

    @Inject
    private WizardService wizardService;

    @Context
    private HttpServletRequest request;

    @POST
    public Response create(Game entity) throws Exception {
        return Response.ok(wizardService.createGame(getUserId(), entity, request)).build();
    }

    @Path("copy-from/{sourceGameId}")
    @POST
    public Response copyFrom(
            @PathParam("sourceGameId") long sourceGameId, WizardCopyRequest entity) throws Exception {
        var state = wizardService.copyGame(getUserId(), sourceGameId, entity, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}")
    @GET
    public Response get(@PathParam("gameId") long gameId) throws Exception {
        var state = wizardService.getState(getUserId(), gameId);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}")
    @DELETE
    public Response remove(@PathParam("gameId") long gameId) throws Exception {
        if (!wizardService.removeGame(getUserId(), gameId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{gameId}/settings")
    @PUT
    public Response updateSettings(@PathParam("gameId") long gameId, Game entity) throws Exception {
        var state = wizardService.updateSettings(getUserId(), gameId, entity, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/players")
    @POST
    public Response addPlayers(
            @PathParam("gameId") long gameId, List<WizardMemberRequest> players) throws Exception {
        var state = wizardService.addPlayers(getUserId(), gameId, players, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/players/reusable")
    @GET
    public Response getReusablePlayers(@PathParam("gameId") long gameId) throws Exception {
        var players = wizardService.getReusablePlayers(getUserId(), gameId);
        if (players == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(players).build();
    }

    @Path("{gameId}/players/reuse")
    @POST
    public Response reusePlayers(
            @PathParam("gameId") long gameId, List<GameMember> players) throws Exception {
        var state = wizardService.reusePlayers(getUserId(), gameId, players, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/players/{memberId}")
    @PUT
    public Response updatePlayer(
            @PathParam("gameId") long gameId, @PathParam("memberId") long memberId,
            GameMember player) throws Exception {
        var state = wizardService.updatePlayer(getUserId(), gameId, memberId, player, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/players/{memberId}/password")
    @PUT
    public Response updatePassword(
            @PathParam("gameId") long gameId, @PathParam("memberId") long memberId,
            WizardPasswordRequest password) throws Exception {
        var state = wizardService.updatePassword(getUserId(), gameId, memberId, password, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/players/{memberId}")
    @DELETE
    public Response removePlayer(
            @PathParam("gameId") long gameId, @PathParam("memberId") long memberId) throws Exception {
        var state = wizardService.removePlayer(getUserId(), gameId, memberId, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/geofences")
    @POST
    public Response addGeofences(
            @PathParam("gameId") long gameId, List<GameGeofence> geofences) throws Exception {
        var state = wizardService.addGeofences(getUserId(), gameId, geofences, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/geofences/{gameGeofenceId}")
    @PUT
    public Response updateGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId,
            GameGeofence geofence) throws Exception {
        var state = wizardService.updateGeofence(getUserId(), gameId, gameGeofenceId, geofence, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

    @Path("{gameId}/geofences/{gameGeofenceId}")
    @DELETE
    public Response removeGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId) throws Exception {
        var state = wizardService.removeGeofence(getUserId(), gameId, gameGeofenceId, request);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

}
