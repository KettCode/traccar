package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.api.TraccarException;
import org.traccar.manhunt.*;
import org.traccar.manhunt.ManhuntInfo;
import org.traccar.model.*;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
    
    @Path("getHuntedDevices")
    @GET
    public Collection<DeviceInfo> getHuntedDevices() throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            return new ArrayList<>();

        return manhuntDatabaseStorage.getHuntedDevices(manhunt.getId(),false);
    }

    @Path("getManhuntInfo")
    @GET
    public Response getManhuntInfo() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var manhuntInfo = new ManhuntInfo();
        manhuntInfo.setManhunt(manhunt);

        var huntedDevices = manhuntDatabaseStorage.getHuntedDevices(manhunt.getId(), true);
        manhuntInfo.setHuntedDevices(huntedDevices);

        return Response.ok(manhuntInfo).build();
    }

    @Path("getSpeedHuntInfo")
    @GET
    public Response getSpeedHuntInfo() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var dto = manhuntDatabaseStorage.getSpeedHuntDtos(manhunt.getId());

        var speedHuntIds = dto.stream().map(SpeedHunt::getId).toList();
        var speedHuntRequests = manhuntDatabaseStorage.getSpeedHuntRequests(speedHuntIds);
        dto.forEach(x -> {
            var speedHuntRequestsInternal = speedHuntRequests.stream()
                    .filter(y -> y.getSpeedHuntsId() == x.getId())
                    .toList();
            x.setSpeedHuntRequests(speedHuntRequestsInternal);
        });
        var speedHuntInfo = new SpeedHuntInfo();
        speedHuntInfo.setManhunt(manhunt);
        speedHuntInfo.setSpeedHunts(dto);
        return Response.ok(speedHuntInfo).build();
    }

    @Path("createCatch")
    @POST
    public Response createCatch(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        var user = permissionsService.getUser(getUserId(), true);
        if(user.getGroup() == null || user.getGroup().getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var dto = manhuntDatabaseStorage.getCreateCatchDto(deviceId);
        CheckCreateCatchDto(dto);

        var catch1 = manhuntDatabaseStorage.createCatch(dto.getManhuntId(), user.getGroupId(), deviceId);
        sendCatchEvent(dto.getDeviceId(), dto.getDeviceName(), user.getGroup());

        return Response.ok(catch1).build();
    }

    @Path("createSpeedHunt")
    @POST
    public Response createSpeedHunt(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var user = permissionsService.getUser(getUserId(), true);
        if(user.getGroup() == null || user.getGroup().getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var dto = manhuntDatabaseStorage.getCreateSpeedHuntDto(deviceId);
        CheckCreateSpeedHuntDto(dto, user, deviceId);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);

        var speedHunt = manhuntDatabaseStorage.createSpeedHunt(dto.getManhuntId(), user.getGroup().getId(), deviceId);
        manhuntDatabaseStorage.createSpeedHuntRequest(speedHunt.getId(), getUserId());
        connectionManager.updateAllPosition(true, position);
        sendSpeedHuntEvent(dto.getDeviceId(), dto.getDeviceName(), user.getGroup(), position);

        return Response.ok(speedHunt).build();
    }

    @Path("createSpeedHuntRequest")
    @POST
    public Response createSpeedHuntRequest(@QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var user = permissionsService.getUser(getUserId(), true);
        if(user.getGroup() == null || user.getGroup().getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var dto = manhuntDatabaseStorage.getCreateSpeedHuntRequestDto(speedHuntId);
        CheckCreateSpeedHuntRequestDto(dto, user);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(dto.getDeviceId())));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);
        var speedHuntRequest = manhuntDatabaseStorage.createSpeedHuntRequest(dto.getSpeedHuntId(), getUserId());
        connectionManager.updateAllPosition(true, position);
        sendSpeedHuntRequestEvent(dto.getDeviceId(), dto.getDeviceName(), user.getGroup(), position);

        return Response.ok(speedHuntRequest).build();
    }

    private void CheckManhunt(IContainsManhunt dto) throws TraccarException {
        if(dto == null || dto.getManhuntId() == 0)
            throw new TraccarException("Es wurde kein Spiel gefunden.");
    }

    private void CheckDeviceDto(IContainsDevice dto) throws TraccarException {
        if(dto.getDeviceId() == 0)
            throw new TraccarException("Das ausgewählte Gerät konnte nicht gefunden werden.");

        if(dto.getDeviceManhuntRole() != 2)
            throw new TraccarException("Das ausgewählte Gerät ist kein 'Gejagter'.");

        if(dto.getDeviceIsCaught())
            throw new TraccarException("Der Spieler wurde bereits verhaftet.");
    }

    private void CheckCreateCatchDto(CreateCatchDto dto) throws TraccarException {
        CheckManhunt(dto);
        CheckDeviceDto(dto);
    }

    private void CheckCreateSpeedHuntDto(CreateSpeedHuntDto dto, User user, long deviceId) throws TraccarException {
        CheckManhunt(dto);
        CheckDeviceDto(dto);

        //CheckLast
        if(dto.getLastSpeedHuntId() > 0
                && (!dto.getLastDeviceIsCaught()
                        && (dto.getLastSpeedHuntRequests() < user.getGroup().getSpeedHuntRequests())))
            throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

        if(dto.getLastDeviceId() == deviceId)
            throw new TraccarException("Zwei aufeinanderfolgende Speedhunts auf den selben Spieler sind nicht erlaubt.");

        //Check
        if(dto.getSpeedHunts() >= user.getGroup().getSpeedHunts())
            throw new TraccarException("Es gibt keinen verfügbaren Speedhunt mehr.");
    }

    private void CheckCreateSpeedHuntRequestDto(CreateSpeedHuntRequestDto dto, User user) throws TraccarException {
        CheckManhunt(dto);
        CheckDeviceDto(dto);

        if(dto.getSpeedHuntId() == 0)
            throw new TraccarException("Es wurde kein Speedhunt gefunden.");

        if(dto.getSpeedHuntRequests() >= user.getGroup().getSpeedHuntRequests())
            throw new TraccarException("Es gibt keine verfügbare Standortanfrage mehr.");
    }

    private void sendSpeedHuntEvent(long deviceId, String deviceName, Group group, Position position) throws StorageException {
        Event event = new Event();
        event.setDeviceId(deviceId);
        event.setType("speedHunt");
        event.setEventTime(new Date());
        event.setPositionId(position.getId());
        event.set("message", "Speedhunt auf '" + deviceName + "' gestartet");
        event.set("name", "Speedhunt");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }

    private void sendSpeedHuntRequestEvent(long deviceId, String deviceName, Group group, Position position) throws StorageException {
        Event event = new Event();
        event.setDeviceId(deviceId);
        event.setType("speedHuntRequest");
        event.setEventTime(new Date());
        event.setPositionId(position.getId());
        event.set("message", "Standort von '" + deviceName + "' angefragt");
        event.set("name", "Standortanfrage");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }

    private void sendCatchEvent(long deviceId, String deviceName, Group group) throws StorageException {
        Event event = new Event();
        event.setDeviceId(deviceId);
        event.setType("catch");
        event.setEventTime(new Date());
        event.set("message", "Der Spieler '" + deviceName + "' wurde verhaftet");
        event.set("name", "Verhaftung");
        event.set("hunterGroup", group.getName());

        connectionManager.sendEventToAllUsers(event);
    }
}
