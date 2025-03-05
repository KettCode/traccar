package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.api.TraccarException;
import org.traccar.model.*;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;

@Path("currentManhunt")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CurrentManhuntResource extends BaseResource {

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    private ConnectionManager connectionManager;

    public CurrentManhuntResource(){

    }

    @Path("get")
    @GET
    public Response get() throws  StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        return Response.ok(manhunt).build();
    }

    @Path("getGroup")
    @GET
    public Response getGroup() throws  StorageException {
        var group = manhuntDatabaseStorage.getGroupByUserId(getUserId());
        return Response.ok(group).build();
    }

    @Path("getHuntedDevices")
    @GET
    public Collection<Device> getHuntedDevices() throws StorageException {
        return manhuntDatabaseStorage.getHuntedDevices(getUserId());
    }

    @Path("getManhuntHunterInfo")
    @GET
    public Response getManhuntHunterInfo() throws StorageException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var info = manhuntDatabaseStorage.getManhuntHunterInfo(getUserId());
        return Response.ok(info).build();
    }

    @Path("getManhuntHuntedInfo")
    @GET
    public Response getManhuntHuntedInfo() throws StorageException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var info = manhuntDatabaseStorage.getManhuntHuntedInfo(getUserId());
        return Response.ok(info).build();
    }

    @Path("createSpeedHunt")
    @POST
    public Response createSpeedHunt(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var manhuntInfo = manhuntDatabaseStorage.getManhuntHunterInfo(getUserId());
        if(!manhuntInfo.getIsManhuntRunning())
            throw new TraccarException("Es wurde kein laufender Manhunt gefunden.");

        var manhunt = manhuntInfo.getManhunt();
        var group = manhuntInfo.getGroup();
        var speedHunts = manhuntInfo.getSpeedHunts();

        if(manhuntInfo.getIsSpeedHuntRunning())
            throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

        if(speedHunts.size() >= group.getSpeedHunts())
            throw new TraccarException("Das Limit der Speedhunts wurde bereits erreicht.");

        if(manhuntInfo.getLastSpeedHunt() != null && manhuntInfo.getLastSpeedHunt().getDeviceId() == deviceId)
            throw new TraccarException("Zwei aufeinanderfolgende Speedhunts auf den selben Spieler sind nicht erlaubt.");

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        if(device == null)
            throw new TraccarException("Das Zielgerät konnte nicht gefunden werden.");

        var huntedGroup = manhuntDatabaseStorage.getGroupByDeviceId(deviceId);
        if(huntedGroup == null || huntedGroup.getManhuntRole() != 2)
            throw new TraccarException("Dem Zielgerät wurde keine Gruppe mit der Rolle 'Gejagter' zugewiesen.");

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        var time = new Date();

        var speedHunt = new SpeedHunt();
        speedHunt.setManhuntsId(manhuntInfo.getManhunt().getId());
        speedHunt.setHunterGroupId(manhuntInfo.getGroup().getId());
        speedHunt.setDeviceId(deviceId);
        speedHunt.setPos(manhuntInfo.getSpeedHunts().size() + 1);
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

        sendSpeedHuntEvent(device, manhuntInfo.getGroup(), position);

        return Response.ok(speedHunt).build();
    }

    @Path("createSpeedHuntRequest")
    @POST
    public Response createSpeedHuntRequest(@QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var manhuntInfo = manhuntDatabaseStorage.getManhuntHunterInfo(getUserId());
        if(!manhuntInfo.getIsManhuntRunning())
            throw new TraccarException("Es wurde kein laufender Manhunt gefunden.");

        var group = manhuntInfo.getGroup();

        if(!manhuntInfo.getIsSpeedHuntRunning())
            throw new TraccarException("Es gibt keinen aktiven Speedhunt.");

        var speedHunt = manhuntInfo
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
        speedHuntRequest.setPos(speedHunt.getSpeedHuntRequests().size() + 1);
        speedHuntRequest.setId(storage.addObject(speedHuntRequest, new Request(new Columns.Exclude("id"))));

        var userIds = manhuntDatabaseStorage.getUsers(speedHunt.getHunterGroupId())
                .stream().map(User::getId)
                .toList();
        connectionManager.updateHunterPosition(true, position, userIds);

        sendSpeedHuntRequestEvent(device, manhuntInfo.getGroup(), position);

        return Response.ok(speedHuntRequest).build();
    }

    @Path("createCatch")
    @POST
    public Response createCatch(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein laufender Manhunt gefunden.");

        var group = manhuntDatabaseStorage.getGroupByUserId(getUserId());
        if(group == null || group.getManhuntRole() != 1)
            throw new TraccarException("Dem Benutzer wurde keine Gruppe mit der Rolle 'Jaeger' zugewiesen.");

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        if(device == null)
            throw new TraccarException("Das Zielgerät konnte nicht gefunden werden.");

        var huntedGroup = manhuntDatabaseStorage.getGroupByDeviceId(deviceId);
        if(huntedGroup == null || huntedGroup.getManhuntRole() != 2)
            throw new TraccarException("Dem Zielgerät wurde keine Gruppe mit der Rolle 'Gejagter' zugewiesen.");

        var currentCatch = storage.getObject(Catches.class, new Request(new Columns.All(),
                new Condition.And(
                        new Condition.Equals("manhuntsId", manhunt.getId()),
                        new Condition.Equals("deviceId", deviceId)
                )));
        if(currentCatch != null)
            throw new TraccarException("Der Spieler wurde bereits gefangen");

        var time = new Date();

        var catch1 = new Catches();
        catch1.setManhuntsId(manhunt.getId());
        catch1.setHunterGroupId(group.getId());
        catch1.setDeviceId(deviceId);
        catch1.setTime(time);
        catch1.setId(storage.addObject(catch1, new Request(new Columns.Exclude("id"))));

        sendCatchEvent(device, group);

        return Response.ok(catch1).build();
    }

    private void sendSpeedHuntEvent(Device device, Group group, Position position) throws StorageException {
        Event event = new Event();
        event.setDeviceId(device.getId());
        event.setType("speedHunt");
        event.setEventTime(new Date());
        event.setPositionId(position.getId());
        event.set("message", "Speedhunt auf '" + device.getName() + "' gestartet");
        event.set("name", "Speedhunt");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }

    private void sendSpeedHuntRequestEvent(Device device, Group group, Position position) throws StorageException {
        Event event = new Event();
        event.setDeviceId(device.getId());
        event.setType("speedHuntRequest");
        event.setEventTime(new Date());
        event.setPositionId(position.getId());
        event.set("message", "Standort von '" + device.getName() + "' angefragt");
        event.set("name", "Standort");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }

    private void sendCatchEvent(Device device, Group group) throws StorageException {
        Event event = new Event();
        event.setDeviceId(device.getId());
        event.setType("catch");
        event.setEventTime(new Date());
        event.set("message", "'" + device.getName() + "' wurde gefangen");
        event.set("name", "Catch");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }
}
