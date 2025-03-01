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

@Path("catches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatchesResource extends BaseObjectResource<Catches> {

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    private ConnectionManager connectionManager;


    public CatchesResource() {
        super(Catches.class);
    }

    @GET
    public Collection<Catches> get(
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

    @Path("getForManhunt")
    @GET
    public Collection<Catches> getForManhunt() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein laufender Manhunt gefunden.");

        return storage.getObjects(baseClass, new Request(
                new Columns.All(), new Condition.Equals("manhuntsId", manhunt.getId())));
    }

    @Path("create")
    @POST
    public Response create(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein laufender Manhunt gefunden.");

        var group = manhuntDatabaseStorage.getHunterGroup(getUserId());
        if(group == null)
            throw new TraccarException("Dem Benutzer wurde keine Gruppe mit der Rolle 'Jaeger' zugewiesen.");

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        if(device == null)
            throw new TraccarException("Das Zielgerät konnte nicht gefunden werden.");

        var huntedGroup = manhuntDatabaseStorage.getHuntedGroup(deviceId);
        if(huntedGroup == null)
            throw new TraccarException("Dem Zielgerät wurde keine Gruppe mit der Rolle 'Gejagter' zugewiesen.");

        var catches = manhuntDatabaseStorage.getCatches(manhunt.getId());
        if(catches.stream().anyMatch(x -> x.getDeviceId() == deviceId))
            throw new TraccarException("Der Spieler wurde bereits gefangen");

        var time = new Date();

        var catch1 = new Catches();
        catch1.setManhuntsId(manhunt.getId());
        catch1.setHunterGroupId(group.getId());
        catch1.setDeviceId(deviceId);
        catch1.setTime(time);
        catch1.setId(storage.addObject(catch1, new Request(new Columns.Exclude("id"))));

        var allUserIds = manhuntDatabaseStorage.getAllUsers()
                .stream().map(User::getId)
                .toList();

        Event event = new Event();
        event.setDeviceId(deviceId);
        event.setType("catch");
        event.setEventTime(new Date());
        event.set("message", "'" + device.getName() + "' wurde gefangen.");
        event.set("name", "Catch");
        event.set("hunterGroup", group.getName());

        allUserIds.forEach(userId -> {
            connectionManager.updateEvent(true, userId, event);
        });

        return Response.ok(catch1).build();
    }
}
