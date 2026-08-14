package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.SetupGameGeofenceService;
import org.traccar.model.GameGeofence;

import java.util.List;

@Path("setup/games/{gameId}/geofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupGameGeofenceResource extends BaseResource {

    @Inject
    private SetupGameGeofenceService setupGameGeofenceService;

    @Context
    private HttpServletRequest request;

    @POST
    public Response addGeofences(
            @PathParam("gameId") long gameId, List<GameGeofence> geofences) throws Exception {
        if (!setupGameGeofenceService.addGeofences(getUserId(), gameId, geofences, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{gameGeofenceId}")
    @PUT
    public Response updateGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId,
            GameGeofence geofence) throws Exception {
        if (!setupGameGeofenceService.updateGeofence(getUserId(), gameId, gameGeofenceId, geofence, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @Path("{gameGeofenceId}")
    @DELETE
    public Response removeGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId) throws Exception {
        if (!setupGameGeofenceService.removeGeofence(getUserId(), gameId, gameGeofenceId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
