package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
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
        hunterQuery.setId(1);
        hunterQuery.setName("Hunterstandorte erfragen");

        var skipLocation = new KeyValue();
        skipLocation.setId(2);
        skipLocation.setName("Nächste Position aussetzten");

        var revealSpeedHunt = new KeyValue();
        revealSpeedHunt.setId(3);
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
        available.setId(1);
        available.setName("Verfügbar");

        var used = new KeyValue();
        used.setId(2);
        used.setName("Benutzt");

        var lst = new HashSet<KeyValue>();
        lst.add(available);
        lst.add(used);

        return lst;
    }
}
