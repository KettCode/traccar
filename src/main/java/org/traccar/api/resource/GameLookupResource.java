package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.BaseResource;
import org.traccar.game.GameLookupService;
import org.traccar.game.view.GameLookupOption;
import org.traccar.game.view.GameLookups;

import java.util.List;

@Path("games/lookups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameLookupResource extends BaseResource {

    @Inject
    private GameLookupService gameLookupService;

    @GET
    public GameLookups get() {
        return gameLookupService.getLookups();
    }

    @Path("gameStatuses")
    @GET
    public List<GameLookupOption> getGameStatuses() {
        return gameLookupService.getGameStatuses();
    }

    @Path("memberRoles")
    @GET
    public List<GameLookupOption> getMemberRoles() {
        return gameLookupService.getMemberRoles();
    }

    @Path("memberStatuses")
    @GET
    public List<GameLookupOption> getMemberStatuses() {
        return gameLookupService.getMemberStatuses();
    }

    @Path("geofenceTypes")
    @GET
    public List<GameLookupOption> getGeofenceTypes() {
        return gameLookupService.getGeofenceTypes();
    }

    @Path("geofenceStatuses")
    @GET
    public List<GameLookupOption> getGeofenceStatuses() {
        return gameLookupService.getGeofenceStatuses();
    }

    @Path("jokerTypes")
    @GET
    public List<GameLookupOption> getJokerTypes() {
        return gameLookupService.getJokerTypes();
    }

    @Path("jokerStatuses")
    @GET
    public List<GameLookupOption> getJokerStatuses() {
        return gameLookupService.getJokerStatuses();
    }

    @Path("pingSources")
    @GET
    public List<GameLookupOption> getPingSources() {
        return gameLookupService.getPingSources();
    }

    @Path("revealTypes")
    @GET
    public List<GameLookupOption> getRevealTypes() {
        return gameLookupService.getRevealTypes();
    }

    @Path("catchStatuses")
    @GET
    public List<GameLookupOption> getCatchStatuses() {
        return gameLookupService.getCatchStatuses();
    }

    @Path("pendingEffectTypes")
    @GET
    public List<GameLookupOption> getPendingEffectTypes() {
        return gameLookupService.getPendingEffectTypes();
    }

}
