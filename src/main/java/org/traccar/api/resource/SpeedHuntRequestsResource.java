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

@Path("speedHuntRequests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpeedHuntRequestsResource extends BaseObjectResource<SpeedHuntRequest> {

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    private ConnectionManager connectionManager;

    public SpeedHuntRequestsResource(){
        super(SpeedHuntRequest.class);
    }

    @GET
    public Collection<SpeedHuntRequest> get(
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

    @Path("create")
    @POST
    public Response create(@QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException {
        var speedHuntInfo = manhuntDatabaseStorage.getSpeedHuntInfo(getUserId());
        var group = speedHuntInfo.getGroup();

        if(!speedHuntInfo.getIsSpeedHuntRunning())
            throw new TraccarException("Es gibt keinen aktiven Speedhunt.");

        var speedHunt = speedHuntInfo
                .getSpeedHunts()
                .stream()
                .filter(x -> x.getId() == speedHuntId)
                .findFirst()
                .orElse(null);

        if(speedHunt == null)
            throw new TraccarException("Es wurde kein Speedhunt gefunden.");

        var speedHuntRequests = speedHunt.getSpeedHuntRequests();
        if(speedHuntRequests.size() >= group.getSpeedHuntRequests())
            throw new TraccarException("Das Limit der Standortanfragen wurde bereits erreicht.");

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", speedHunt.getDeviceId())));
        if(device == null)
            throw new TraccarException("Das Zielgerät konnte nicht gefunden werden.");

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(speedHunt.getDeviceId())));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        var time = new Date();

        speedHunt.setLastTime(time);
        storage.updateObject(speedHunt, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", speedHunt.getId())));

        var speedHuntRequest = new SpeedHuntRequest();
        speedHuntRequest.setSpeedHuntsId(speedHunt.getId());
        speedHuntRequest.setUserId(getUserId());
        speedHuntRequest.setTime(time);
        speedHuntRequest.setPos(speedHuntRequests.size() + 1);
        speedHuntRequest.setId(storage.addObject(speedHuntRequest, new Request(new Columns.Exclude("id"))));

        var userIds = manhuntDatabaseStorage.getUsers(speedHunt.getHunterGroupId())
                .stream().map(User::getId)
                .toList();
        connectionManager.updateHunterPosition(true, position, userIds);

        var allUserIds = manhuntDatabaseStorage.getAllUsers()
                .stream().map(User::getId)
                .toList();

        Event event = new Event();
        event.setDeviceId(device.getId());
        event.setType("speedHuntRequest");
        event.setEventTime(new Date());
        event.setPositionId(position.getId());
        event.set("message", "Standort von '" + device.getName() + "' angefragt.");
        event.set("name", "Standort");
        event.set("hunterGroup", group.getName());

        allUserIds.forEach(userId -> {
            connectionManager.updateEvent(true, userId, event);
        });

        return Response.ok(speedHuntRequest).build();
    }
}
