package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.SetupGeofenceService;

@Path("setup/geofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupGeofenceResource extends BaseResource {

    @Inject
    private SetupGeofenceService setupGeofenceService;

    @Context
    private HttpServletRequest request;

    @Path("{geofenceId}")
    @DELETE
    public Response remove(@PathParam("geofenceId") long geofenceId) throws Exception {
        if (!setupGeofenceService.removeGeofence(getUserId(), geofenceId, request)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
