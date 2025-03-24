package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.api.TraccarException;
import org.traccar.manhunt.info.ManhuntInfo;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.manhunt.dto.SpeedHuntDto;
import org.traccar.manhunt.info.SpeedHuntInfo;
import org.traccar.model.*;
import org.traccar.notification.NotificationMessage;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

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
    public Collection<DeviceDto> getHuntedDevices() throws StorageException {
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

        var dto = manhuntDatabaseStorage.getSpeedHunts(manhunt.getId());

        var speedHuntIds = dto.stream().map(SpeedHunt::getId).toList();
        var speedHuntRequests = manhuntDatabaseStorage.getSpeedHuntRequests(speedHuntIds);
        dto.forEach(x -> {
            var speedHuntRequestsInternal = speedHuntRequests.stream()
                    .filter(y -> y.getSpeedHuntsId() == x.getId())
                    .toList();
            x.setSpeedHuntRequests(speedHuntRequestsInternal);
            var availableRequests = manhunt.getLocationRequests() - speedHuntRequestsInternal.size();
            x.setAvailableSpeedHuntRequests(availableRequests);
        });

        var speedHuntInfo = new SpeedHuntInfo();
        speedHuntInfo.setManhunt(manhunt);
        speedHuntInfo.setSpeedHunts(dto);
        return Response.ok(speedHuntInfo).build();
    }

    @Path("createCatch")
    @POST
    public Response createCatch(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        var user = permissionsService.getUser(getUserId());
        if(user.getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var dto = manhuntDatabaseStorage.getDevice(manhunt.getId(), deviceId);
        CheckDevice(dto);

        var catch1 = manhuntDatabaseStorage.createCatch(manhunt.getId(), getUserId(), deviceId);
        sendCatchNotification(dto);

        return Response.ok(catch1).build();
    }

    @Path("createSpeedHunt")
    @POST
    public Response createSpeedHunt(@QueryParam("deviceId") long deviceId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var user = permissionsService.getUser(getUserId());
        if(user.getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var lastSpeedHuntDto = manhuntDatabaseStorage.getLastSpeedHunt(manhunt.getId());
        CheckSpeedHunt(lastSpeedHuntDto, deviceId, manhunt);

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), deviceId);
        CheckDevice(deviceDto);

        var numSpeedHunts = storage.getObjects(SpeedHunt.class, new Request(
                new Columns.All(),
                new Condition.Equals("manhuntsId", manhunt.getId())))
                .size();

        if(numSpeedHunts >= manhunt.getSpeedHunts())
            throw new TraccarException("Es gibt keinen verfügbaren Speedhunt mehr.");

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);

        var speedHunt = manhuntDatabaseStorage.createSpeedHunt(manhunt.getId(), getUserId(), deviceId);
        manhuntDatabaseStorage.createSpeedHuntRequest(speedHunt.getId(), getUserId());
        connectionManager.updateAllPosition(true, position);
        sendSpeedHuntNotification(numSpeedHunts + 1, manhunt.getSpeedHunts());

        return Response.ok(speedHunt).build();
    }

    @Path("createSpeedHuntRequest")
    @POST
    public Response createSpeedHuntRequest(@QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var user = permissionsService.getUser(getUserId());
        if(user.getManhuntRole() != 1)
            throw new TraccarException("Der Benutzer ist kein 'Jaeger'.");

        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var lastSpeedHuntDto = manhuntDatabaseStorage.getLastSpeedHunt(manhunt.getId());
        CheckSpeedHunt(lastSpeedHuntDto, speedHuntId, user, manhunt);

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), lastSpeedHuntDto.getDeviceId());
        CheckDevice(deviceDto);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(lastSpeedHuntDto.getDeviceId())));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);
        var speedHuntRequest = manhuntDatabaseStorage.createSpeedHuntRequest(lastSpeedHuntDto.getId(), getUserId());
        connectionManager.updateAllPosition(true, position);
        sendSpeedHuntRequestNotification(lastSpeedHuntDto.getNumRequests() + 1, manhunt.getLocationRequests());

        return Response.ok(speedHuntRequest).build();
    }

    private void CheckDevice(DeviceDto dto) throws TraccarException {
        if(dto == null || dto.getId() == 0)
            throw new TraccarException("Das Gerät konnte nicht gefunden werden.");

        if(dto.getManhuntRole() != 2)
            throw new TraccarException("Das Gerät ist kein 'Gejagter'.");

        if(dto.getIsCaught())
            throw new TraccarException("Das Gerät wurde bereits verhaftet.");
    }

    private void CheckSpeedHunt(SpeedHuntDto dto, long deviceId, Manhunt manhunt) throws TraccarException {
        if(dto == null)
            return;

        if(!dto.getDeviceIsCaught() && (dto.getNumRequests() < manhunt.getLocationRequests()))
            throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

        if(dto.getDeviceId() == deviceId)
            throw new TraccarException("Zwei aufeinanderfolgende Speedhunts auf den selben Spieler sind nicht erlaubt.");
    }

    private void CheckSpeedHunt(SpeedHuntDto dto, long speedHuntId, User user, Manhunt manhunt) throws TraccarException {
        if(dto == null || dto.getId() == 0)
            throw new TraccarException("Es wurde kein Speedhunt gefunden.");

        if(dto.getId() != speedHuntId)
            throw new TraccarException("Der Speedhunt ist veraltet.");

        if(dto.getNumRequests() >= manhunt.getLocationRequests())
            throw new TraccarException("Es gibt keine verfügbare Standortanfrage mehr.");
    }

    private void sendSpeedHuntNotification(long numSpeedHunt, long maxSpeedHunts) throws StorageException {
        var notificationMessage = new NotificationMessage("Speedhunt", "Speedhunt " + numSpeedHunt + "/" + maxSpeedHunts + " gestartet!");
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }

    private void sendSpeedHuntRequestNotification(long numRequests, long maxRequests) throws StorageException {
        var notificationMessage = new NotificationMessage("Standortanfrage", "Standort " + numRequests + "/" + maxRequests + " angefragt!");
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }

    private void sendCatchNotification(Device device) throws StorageException {
        var notificationMessage = new NotificationMessage("Verhaftung", "Der Spieler '" + device.getName() + "' wurde verhaftet");
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }
}
