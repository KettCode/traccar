package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseObjectResource;
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
    public Response create(@QueryParam("speedHuntId") long speedHuntId) throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new RuntimeException("Es wurde kein laufender Manhunt gefunden.");

        var group = manhuntDatabaseStorage.getHunterGroup(getUserId());
        if(group == null)
            throw new RuntimeException("Dem Benutzer wurde keine Gruppe mit der Rolle 'Jaeger' zugewiesen.");

        var speedHunt = manhuntDatabaseStorage.getSpeedHunt(speedHuntId);
        if(speedHunt == null)
            throw new RuntimeException("Es wurde kein Speedhunt gefunden.");

        var speedHuntRequests = manhuntDatabaseStorage.getSpeedHuntRequests(speedHuntId);
        if(speedHuntRequests.size() >= group.getSpeedHuntRequests())
            throw new RuntimeException("Das Limit der Standortanfragen wurde bereits erreicht.");

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

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(speedHunt.getDeviceId())));
        var userIds = manhuntDatabaseStorage.getUsers(speedHunt.getHunterGroupId())
                .stream().map(User::getId)
                .toList();
        connectionManager.updateSpeedHuntPosition(true, position, userIds);

        return Response.ok(speedHuntRequest).build();
    }
}
