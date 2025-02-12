package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.*;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.management.Query;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Path("speedhunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpeedhuntResource extends BaseObjectResource<SpeedHunt> {

    public SpeedhuntResource() {
        super(SpeedHunt.class);
    }

    @GET
    public Collection<SpeedHunt> get(
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
    public Response Create(SpeedHunt entity) throws StorageException {
        Device device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", entity.getDeviceId())));

        if(device.getGroupId() <= 0)
            throw new RuntimeException("Dem Gerät wurde keine Gruppe zugewiesen");

        var manhunt = storage.getObject(Manhunt.class,
                new Request(new Columns.All(), new Condition.Equals("groupId", device.getGroupId())));
        var speedhunts = storage.getObjects(SpeedHunt.class,
                new Request(new Columns.All(), new Condition.Equals("manhuntsId", manhunt.getId())));

        //if(speedhunts.size() >= manhunt.getSpeedHuntLimit())
        //    throw new RuntimeException("Das Limit für die Speedhunts ist erreicht.");

        var speedhunt = new SpeedHunt();
        speedhunt.setDeviceId(entity.getDeviceId());
        speedhunt.setLastTime(entity.getLastTime());
        speedhunt.setPos(speedhunts.size() + 1);
        speedhunt.setManhuntsId(manhunt.getId());
        speedhunt.setId(storage.addObject(speedhunt, new Request(new Columns.Exclude("id"))));

        var speedhuntRequest = new SpeedHuntRequest();
        speedhuntRequest.setPos(1);
        speedhuntRequest.setSpeedHuntsId(speedhunt.getId());
        speedhuntRequest.setTime(entity.getLastTime());
        speedhuntRequest.setUserId(getUserId());
        speedhuntRequest.setId(storage.addObject(speedhuntRequest, new Request(new Columns.Exclude("id"))));

        return Response.ok(speedhunt).build();
    }

    @Path("/trigger")
    @POST
    public Response Trigger(SpeedHuntRequest speedhuntRequest) throws StorageException {
        var speedhunt = storage.getObject(SpeedHunt.class,
                new Request(new Columns.All(), new Condition.Equals("id", speedhuntRequest.getSpeedHuntsId())));
        var manhunt = storage.getObject(Manhunt.class,
                new Request(new Columns.All(), new Condition.Equals("id", speedhunt.getManhuntsId())));
        var speedhuntRequests = storage.getObjects(SpeedHuntRequest.class,
                new Request(new Columns.All(), new Condition.Equals("speedhuntsId", speedhunt.getId())));

        //if(speedhuntRequests.size() >= manhunt.getSpeedHuntRequests())
        //    throw new RuntimeException("Das Limit für die Speedhuntanfragen wurde erreicht");

        speedhunt.setLastTime(speedhuntRequest.getTime());
        storage.updateObject(speedhunt, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", speedhunt.getId())));

        speedhuntRequest.setPos(speedhuntRequests.size() + 1);
        speedhuntRequest.setUserId(getUserId());
        speedhuntRequest.setId(storage.addObject(speedhuntRequest, new Request(new Columns.Exclude("id"))));

        return Response.ok(speedhuntRequest).build();
    }

    @Path("current")
    @GET
    public Response current() throws StorageException {
        //var speedhuntRequest = storage.getObject(SpeedhuntRequest.class,
        //        new Request(new Columns.All(), new Condition.Equals("speedhuntsId", speedhunt.getId())));

        //storage.getObject(SpeedhuntRequest.class, new Request(new Columns.All(), new Condition.LatestPositions()));
        // Select *
        // from tc_speedhunts
        // join tc_manhunts on tc_manhunts.id = tc_speedhunts.manhuntsId
        // join (
        //      Select tc_shr.speedhuntsId, MAX(tc_shr.pos) as maxPos
        //      from tc_speedhuntRequests tc_shr
        //      where tc_shr.speedhuntsId = tc_speedhunts.id
        //      GROUP BY tc_shr.speedhuntsId
        // ) tc_shr_max on tc_speedhunts.id = tc_shr_max.speedhuntsId
        // join tc_speedhuntRequests
        // where tc_manhunts.speedHuntRequests < (Select Count(*)
        //      from tc_speedhuntRequests
        //      where tc_speedhuntRequests.speedhuntsId = tc_speedhunts.id)

        var request = new SpeedHuntRequest();
        return Response.ok(request).build();
    }

    //private Manhunt GetCurrent() {
    //    List<Manhunt> manhunts = storage.getObjects(Manhunt.class);

        //String sql = "SELECT * FROM tc_manhunts WHERE start < CURRENT_TIMESTAMP ORDER BY start DESC LIMIT 1";
        //Query query = entityManager.createNativeQuery(sql, Manhunt.class);
        //return (Manhunt) query.getSingleResult();
    //}
}
