package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.*;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

@Path("speedhunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpeedhuntResource extends BaseObjectResource<Speedhunt> {
    //private final Storage storage;
    //private final long userId;

    public SpeedhuntResource() {
        super(Speedhunt.class);
    }

    @GET
    public Collection<Speedhunt> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("groupId") long groupId, @QueryParam("deviceId") long deviceId) throws StorageException {

        var conditions = new LinkedList<Condition>();

        if (all) {
            if (permissionsService.notAdmin(getUserId())) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            }
        } else {
            if (userId == 0) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            } else {
                permissionsService.checkUser(getUserId(), userId);
                conditions.add(new Condition.Permission(User.class, userId, baseClass).excludeGroups());
            }
        }

        if (groupId > 0) {
            permissionsService.checkPermission(Group.class, getUserId(), groupId);
            conditions.add(new Condition.Permission(Group.class, groupId, baseClass).excludeGroups());
        }
        if (deviceId > 0) {
            permissionsService.checkPermission(Device.class, getUserId(), deviceId);
            conditions.add(new Condition.Permission(Device.class, deviceId, baseClass).excludeGroups());
        }

        return storage.getObjects(baseClass, new Request(
                new Columns.All(), Condition.merge(conditions), null));
    }

    @Path("/create")
    @POST
    public Response Create(Speedhunt entity) throws StorageException {
        Device device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", entity.getDeviceId())));

        if(device.getGroupId() <= 0)
            throw new RuntimeException("Dem Gerät wurde keine Gruppe zugewiesen");

        var manhunt = storage.getObject(Manhunt.class,
                new Request(new Columns.All(), new Condition.Equals("groupId", device.getGroupId())));
        var speedhunts = storage.getObjects(Speedhunt.class,
                new Request(new Columns.All(), new Condition.Equals("manhuntsId", manhunt.getId())));

        if(speedhunts.size() >= manhunt.getSpeedHuntLimit())
            throw new RuntimeException("Das Limit für die Speedhunts ist erreicht.");

        var speedhunt = new Speedhunt();
        speedhunt.setDeviceId(entity.getDeviceId());
        speedhunt.setLastTime(entity.getLastTime());
        speedhunt.setPos(speedhunts.size() + 1);
        speedhunt.setManhuntsId(manhunt.getId());
        speedhunt.setId(storage.addObject(speedhunt, new Request(new Columns.Exclude("id"))));

        var speedhuntRequest = new SpeedhuntRequest();
        speedhuntRequest.setPos(1);
        speedhuntRequest.setSpeedhuntsid(speedhunt.getId());
        speedhuntRequest.setTime(entity.getLastTime());
        speedhuntRequest.setUserId(getUserId());
        speedhuntRequest.setId(storage.addObject(speedhuntRequest, new Request(new Columns.Exclude("id"))));

        return Response.ok(speedhunt).build();
    }

    @Path("/trigger")
    @POST
    public Response Trigger(SpeedhuntRequest speedhuntRequest) throws StorageException {
        var speedhunt = storage.getObject(Speedhunt.class,
                new Request(new Columns.All(), new Condition.Equals("id", speedhuntRequest.getSpeedhuntsid())));
        var manhunt = storage.getObject(Manhunt.class,
                new Request(new Columns.All(), new Condition.Equals("id", speedhunt.getManhuntsId())));
        var speedhuntRequests = storage.getObjects(SpeedhuntRequest.class,
                new Request(new Columns.All(), new Condition.Equals("speedhuntsId", speedhunt.getId())));

        if(speedhuntRequests.size() >= manhunt.getSpeedHuntRequests())
            throw new RuntimeException("Das Limit für die Speedhuntanfragen wurde erreicht");

        speedhunt.setLastTime(speedhuntRequest.getTime());
        storage.updateObject(speedhunt, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", speedhunt.getId())));

        speedhuntRequest.setPos(speedhuntRequests.size() + 1);
        speedhuntRequest.setUserId(getUserId());
        speedhuntRequest.setId(storage.addObject(speedhuntRequest, new Request(new Columns.Exclude("id"))));

        return Response.ok(speedhuntRequest).build();
    }
}
