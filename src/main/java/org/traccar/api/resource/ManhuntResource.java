package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.*;
import org.traccar.session.ConnectionManager;

import java.util.Collection;
import java.util.HashSet;

@Path("manhunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManhuntResource extends ExtendedObjectResource<Manhunt> {

    @Inject
    private ConnectionManager connectionManager;

    public ManhuntResource() {
        super(Manhunt.class, "start");
    }

    @POST
    public Response add(Manhunt entity) throws Exception {
        var response = super.add(entity);
        connectionManager.initSchedules();
        return response;
    }

    @Path("{id}")
    @PUT
    public Response update(Manhunt entity) throws Exception {
        var response = super.update(entity);
        connectionManager.initSchedules();
        return response;
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        var response = super.remove(id);
        connectionManager.initSchedules();
        return response;
    }

    @GET
    @Path("getRoles")
    public Collection<Role> getRoles(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("groupId") long groupId, @QueryParam("deviceId") long deviceId) {

        var role1 = new Role();
        role1.setId(0);
        role1.setName("No");

        var role2 = new Role();
        role2.setId(1);
        role2.setName("Hunter");

        var role3 = new Role();
        role3.setId(2);
        role3.setName("Hunted");

        var lst = new HashSet<Role>();
        lst.add(role1);
        lst.add(role2);
        lst.add(role3);

        return lst;
    }
}
