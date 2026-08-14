package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.game.setup.wizard.WizardStateService;

@Path("setup/wizard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupWizardResource extends BaseResource {

    @Inject
    private WizardStateService wizardStateService;

    @Path("{gameId}")
    @GET
    public Response get(@PathParam("gameId") long gameId) throws Exception {
        var state = wizardStateService.getState(getUserId(), gameId);
        if (state == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(state).build();
    }

}
