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
import org.traccar.game.geofence.GameGeofenceActionService;

@Path("games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameGeofenceActionResource extends BaseResource {

    @Inject
    private GameGeofenceActionService geofenceActionService;

    @Context
    private HttpServletRequest request;

    @Path("{gameId}/geofences/{gameGeofenceId}/activate")
    @POST
    public Response activateGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId) throws Exception {
        var gameGeofence = geofenceActionService.activateGeofence(getUserId(), gameId, gameGeofenceId, request);
        if (gameGeofence == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(gameGeofence).build();
    }

    @Path("{gameId}/geofences/{gameGeofenceId}/deactivate")
    @POST
    public Response deactivateGeofence(
            @PathParam("gameId") long gameId, @PathParam("gameGeofenceId") long gameGeofenceId) throws Exception {
        var gameGeofence = geofenceActionService.deactivateGeofence(getUserId(), gameId, gameGeofenceId, request);
        if (gameGeofence == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(gameGeofence).build();
    }

}
