package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseObjectResource;
import org.traccar.api.TraccarException;
import org.traccar.model.*;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

@Path("speedHunts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpeedHuntResource extends BaseObjectResource<SpeedHunt> {

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    private ConnectionManager connectionManager;

    public SpeedHuntResource() {
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

    @Path("speedHuntInfo")
    @GET
    public Response speedHuntInfo() throws StorageException, TraccarException {
        var speedHuntInfo = manhuntDatabaseStorage.getSpeedHuntInfo(getUserId());
        return Response.ok(speedHuntInfo).build();
    }

    @Path("create")
    @POST
    public Response create(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        var speedHuntInfo = manhuntDatabaseStorage.getSpeedHuntInfo(getUserId());
        var manhunt = speedHuntInfo.getManhunt();
        var group = speedHuntInfo.getGroup();
        var speedHunts = speedHuntInfo.getSpeedHunts();

        if(speedHuntInfo.getIsSpeedHuntRunning())
            throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

        if(speedHunts.size() >= group.getSpeedHunts())
            throw new TraccarException("Das Limit der Speedhunts wurde bereits erreicht.");

        var huntedGroup = manhuntDatabaseStorage.getHuntedGroup(deviceId);
        if(huntedGroup == null)
            throw new TraccarException("Dem Zielgerät wurde keine Gruppe mit der Rolle 'Gejagter' zugewiesen.");

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        var time = new Date();

        var speedHunt = new SpeedHunt();
        speedHunt.setManhuntsId(manhunt.getId());
        speedHunt.setHunterGroupId(group.getId());
        speedHunt.setDeviceId(deviceId);
        speedHunt.setPos(speedHunts.size() + 1);
        speedHunt.setLastTime(time);
        speedHunt.setId(storage.addObject(speedHunt, new Request(new Columns.Exclude("id"))));

        var speedhuntRequest = new SpeedHuntRequest();
        speedhuntRequest.setSpeedHuntsId(speedHunt.getId());
        speedhuntRequest.setUserId(getUserId());
        speedhuntRequest.setTime(time);
        speedhuntRequest.setPos(1);
        speedhuntRequest.setId(storage.addObject(speedhuntRequest, new Request(new Columns.Exclude("id"))));

        var userIds = manhuntDatabaseStorage.getUsers(speedHunt.getHunterGroupId())
                .stream().map(User::getId)
                .toList();
        connectionManager.updateHunterPosition(true, position, userIds);

        return Response.ok(speedHunt).build();
    }
}
