package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.api.TraccarException;
import org.traccar.manhunt.dto.ManhuntDto;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.manhunt.dto.LastSpeedHuntDto;
import org.traccar.manhunt.dto.SpeedHuntDto;
import org.traccar.model.*;
import org.traccar.notification.NotificationMessage;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

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
    public Response get() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        return Response.ok(manhunt).build();
    }

    @Path("getDevices")
    @GET
    public Collection<DeviceDto> getDevices(@QueryParam("manhuntId") long manhuntId, @QueryParam("manhuntRole") long manhuntRole) throws StorageException {
        var devices = manhuntDatabaseStorage.getDevices(manhuntId);

        if(manhuntRole > 0)
            devices = devices
                    .stream().filter(x -> x.getManhuntRole() == manhuntRole)
                    .toList();

        return devices;
    }

    @Path("getHuntedDevices")
    @GET
    public Collection<DeviceDto> getHuntedDevices() throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            return new ArrayList<>();

        var devices = manhuntDatabaseStorage.getDevices(manhunt.getId());
        return devices
                .stream().filter(x -> x.getManhuntRole() == 2 && !x.getIsCaught())
                .toList();
    }

    @Path("getLastSpeedHunt")
    @GET
    public Response getLastSpeedHunt(@QueryParam("manhuntId") long manhuntId) throws StorageException {
        var lastSpeedHunt = manhuntDatabaseStorage.getLastSpeedHunt(manhuntId);
        return Response.ok(lastSpeedHunt).build();
    }

    @Path("getManhunt")
    @GET
    public Response getManhunt() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

//        var devices = manhuntDatabaseStorage.getDevices(manhunt.getId());
//        manhunt.setDevices(devices);
//
//        var lastSpeedHunt = (SpeedHuntDto) storage.getObject(SpeedHunt.class, new Request(
//                new Columns.All(),
//                new Condition.Equals("manhuntsId", manhunt.getId()),
//                new Order("id", true, 1)));
//
//        if(lastSpeedHunt != null) {
//            var locationRequests =  storage.getObjects(LocationRequest.class, new Request(
//                    new Columns.All(),
//                    new Condition.Equals("spedHuntsId", lastSpeedHunt.getId())
//            ));
//            lastSpeedHunt.setLocationRequests(locationRequests);
//            manhunt.setLastSpeedHunt(lastSpeedHunt);
//        }

        return Response.ok(manhunt).build();
    }

    @Path("getSpeedHuntHistory")
    @GET
    public Response getSpeedHuntHistory() throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        var speedHunts = storage.getObjects(SpeedHunt.class, new Request(
                new Columns.All(),
                new Condition.Equals("manhuntsId", manhunt.getId())));

        var speedHuntIds = speedHunts.stream().map(SpeedHunt::getId).toList();
        var locationRequests = manhuntDatabaseStorage.getSpeedHuntRequests(speedHuntIds);

        var locationRequestMap = locationRequests.stream()
                .collect(Collectors.groupingBy(LocationRequest::getSpeedHuntsId));

        var speedHuntDtos = speedHunts.stream().map(speedHunt -> {
            var speedHuntDto = new SpeedHuntDto();
            var locationRequestsForHunt = locationRequestMap.getOrDefault(speedHunt.getId(), Collections.emptyList());
            speedHuntDto.setLocationRequests(locationRequestsForHunt);
            return speedHuntDto;
        }).collect(Collectors.toList());

        return Response.ok(speedHuntDtos).build();
    }

    @Path("createCatch")
    @POST
    public Response createCatch(@QueryParam("manhuntId") long manhuntId, @QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var dto = manhuntDatabaseStorage.getDevice(manhuntId, deviceId);
        CheckDevice(dto);

        var catch1 = manhuntDatabaseStorage.createCatch(manhuntId, getUserId(), deviceId);
        sendCatchNotification(dto);

        return Response.ok(catch1).build();
    }

    @Path("createSpeedHunt")
    @POST
    public Response createSpeedHunt(@QueryParam("manhuntId") long manhuntId, @QueryParam("deviceId") long deviceId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var manhunt = storage.getObject(Manhunt.class, new Request(
                new Columns.All(),
                new Condition.Equals("id", manhuntId)
        ));

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
        manhuntDatabaseStorage.createLocationRequest(speedHunt.getId(), getUserId());
        connectionManager.updateAllPosition(true, position);
        sendSpeedHuntNotification(numSpeedHunts + 1, manhunt.getSpeedHunts());

        return Response.ok(speedHunt).build();
    }

    @Path("createLocationRequest")
    @POST
    public Response createLocationRequest(@QueryParam("manhuntId") long manhuntId, @QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var manhunt = storage.getObject(Manhunt.class, new Request(
                new Columns.All(),
                new Condition.Equals("id", manhuntId)
        ));

        var lastSpeedHuntDto = manhuntDatabaseStorage.getLastSpeedHunt(manhunt.getId());
        CheckSpeedHuntForLocation(lastSpeedHuntDto, speedHuntId, manhunt);

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), lastSpeedHuntDto.getDeviceId());
        CheckDevice(deviceDto);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(lastSpeedHuntDto.getDeviceId())));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);
        var speedHuntRequest = manhuntDatabaseStorage.createLocationRequest(lastSpeedHuntDto.getId(), getUserId());
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

    private void CheckSpeedHunt(LastSpeedHuntDto dto, long deviceId, Manhunt manhunt) throws TraccarException {
        if(dto == null)
            return;

        if(!dto.getDeviceIsCaught() && (dto.getNumRequests() < manhunt.getLocationRequests()))
            throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

        if(dto.getDeviceId() == deviceId)
            throw new TraccarException("Zwei aufeinanderfolgende Speedhunts auf den selben Spieler sind nicht erlaubt.");
    }

    private void CheckSpeedHuntForLocation(LastSpeedHuntDto dto, long speedHuntId, Manhunt manhunt) throws TraccarException {
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
