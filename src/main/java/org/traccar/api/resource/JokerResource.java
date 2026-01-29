package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.manhunt.JokerStatus;
import org.traccar.manhunt.JokerType;
import org.traccar.model.Joker;
import org.traccar.model.KeyValue;
import org.traccar.model.Role;

import java.util.Collection;
import java.util.HashSet;

@Path("jokers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JokerResource extends ExtendedObjectResource<Joker> {
    public JokerResource() {
        super(Joker.class, "userId");
    }

    @GET
    @Path("getJokerTypes")
    public Collection<KeyValue> getJokerTypes(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("groupId") long groupId, @QueryParam("deviceId") long deviceId) {

        var hunterQuery = new KeyValue();
        hunterQuery.setId(JokerType.HUNTER_QUERY);
        hunterQuery.setName("Jägerstandorte erfragen");

        var skipLocation = new KeyValue();
        skipLocation.setId(JokerType.SKIP_LOCATION);
        skipLocation.setName("Nächste Position aussetzten");

        var revealSpeedHunt = new KeyValue();
        revealSpeedHunt.setId(JokerType.REVEAL_SPEEDHUNT);
        revealSpeedHunt.setName("Speedhunt aufdecken");

        var lst = new HashSet<KeyValue>();
        lst.add(hunterQuery);
        lst.add(skipLocation);
        lst.add(revealSpeedHunt);

        return lst;
    }

    @GET
    @Path("getJokerStates")
    public Collection<KeyValue> getJokerStates(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("groupId") long groupId, @QueryParam("deviceId") long deviceId) {

        var available = new KeyValue();
        available.setId(JokerStatus.AVAILABLE);
        available.setName("Verfügbar");

        var used = new KeyValue();
        used.setId(JokerStatus.USED);
        used.setName("Benutzt");

        var lst = new HashSet<KeyValue>();
        lst.add(available);
        lst.add(used);

        return lst;
    }
}
