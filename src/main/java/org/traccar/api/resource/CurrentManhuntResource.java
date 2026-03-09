package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.api.TraccarException;
import org.traccar.manhunt.JokerType;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.model.*;
import org.traccar.notification.NotificationMessage;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.lang.reflect.InvocationTargetException;
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
    public Response get(@QueryParam("loadCascade") boolean loadCascade) throws StorageException, TraccarException {
        var manhunt = manhuntDatabaseStorage.getCurrent(loadCascade);
        if(manhunt == null)
            throw new TraccarException("Es wurde kein Spiel gefunden.");

        return Response.ok(manhunt).build();
    }

    @Path("getDevices")
    @GET
    public Collection<DeviceDto> getDevices(@QueryParam("manhuntId") long manhuntId,
                                            @QueryParam("huntedOnly") boolean huntedOnly) throws StorageException {
        var devices = manhuntDatabaseStorage.getDevices(manhuntId);

        if(huntedOnly)
            devices = devices
                    .stream().filter(x -> x.getManhuntRole() == 2 && !x.getIsCaught())
                    .toList();

        return devices;
    }

    @Path("getDevice")
    @GET
    public Response getDevice() throws StorageException {
        var device = manhuntDatabaseStorage.getDevice(getUserId());
        return Response.ok(device).build();
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

        var manhunt = manhuntDatabaseStorage.getCurrent(true);

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), deviceId);
        CheckDevice(deviceDto);

        var speedHunts = manhunt.getSpeedHunts();
        if(!speedHunts.isEmpty()) {
            var lastSpeedHunt = manhunt.getSpeedHunts().get(manhunt.getSpeedHunts().size() - 1);

            if(!deviceDto.getIsCaught() && (lastSpeedHunt.getLocationRequests().size() < manhunt.getLocationRequestLimit()))
                throw new TraccarException("Es gibt bereits einen aktiven Speedhunt.");

            if(lastSpeedHunt.getDeviceId() == deviceId)
                throw new TraccarException("Zwei aufeinanderfolgende Speedhunts auf den selben Spieler sind nicht erlaubt.");
        }

        if(manhunt.getSpeedHunts().size() >= manhunt.getSpeedHuntLimit())
            throw new TraccarException("Es gibt keinen verfügbaren Speedhunt mehr.");

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        var speedHunt = manhuntDatabaseStorage.createSpeedHunt(manhunt.getId(), getUserId(), deviceId);
        manhuntDatabaseStorage.createLocationRequest(speedHunt.getId(), getUserId());

        if(deviceDto.getSkipNextManhuntLocation())
            manhuntDatabaseStorage.resetSkipNextLocation(deviceDto.getId());
        else {
            manhuntDatabaseStorage.saveManhuntPosition(position);
            connectionManager.updateAllPosition(true, position);
        }

        sendSpeedHuntNotification(manhunt.getSpeedHunts().size() + 1, manhunt.getSpeedHuntLimit());

        return Response.ok(speedHunt).build();
    }

    @Path("createLocationRequest")
    @POST
    public Response createLocationRequest(@QueryParam("manhuntId") long manhuntId, @QueryParam("speedHuntId") long speedHuntId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var manhunt = manhuntDatabaseStorage.getCurrent(true);
        var speedHunts = manhunt.getSpeedHunts();

        if (speedHunts.isEmpty())
            throw new TraccarException("Es wurde kein Speedhunt gefunden.");

        var lastSpeedHunt = manhunt.getSpeedHunts().get(manhunt.getSpeedHunts().size() - 1);

        if(lastSpeedHunt.getId() != speedHuntId)
            throw new TraccarException("Der Speedhunt ist veraltet.");

        if(lastSpeedHunt.getLocationRequests().size() >= manhunt.getLocationRequestLimit())
            throw new TraccarException("Es gibt keine verfügbare Standortanfrage mehr.");

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), lastSpeedHunt.getDeviceId());
        CheckDevice(deviceDto);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(lastSpeedHunt.getDeviceId())));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        var speedHuntRequest = manhuntDatabaseStorage.createLocationRequest(lastSpeedHunt.getId(), getUserId());

        if(deviceDto.getSkipNextManhuntLocation())
            manhuntDatabaseStorage.resetSkipNextLocation(deviceDto.getId());
        else {
            manhuntDatabaseStorage.saveManhuntPosition(position);
            connectionManager.updateAllPosition(true, position);
        }

        sendSpeedHuntRequestNotification(lastSpeedHunt.getLocationRequests().size() + 1, manhunt.getLocationRequestLimit());

        return Response.ok(speedHuntRequest).build();
    }

    @Path("getJoker")
    @GET
    public Collection<Joker> getJoker(@QueryParam("manhuntId") long manhuntId,
                                      @QueryParam("all") boolean all) throws StorageException {
        var joker = manhuntDatabaseStorage.getJokers(manhuntId);

        if(all) {
            permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());;
            return joker;
        }

        return joker.stream().filter(j -> j.getUserId() == getUserId()).toList();
    }

    @Path("unlockJoker")
    @POST
    public Response unlockJoker(@QueryParam("manhuntId") long manhuntId, @QueryParam("huntedUserId") long huntedUserId, @QueryParam("jokerTypeId") int jokerTypeId) throws StorageException, TraccarException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var joker = manhuntDatabaseStorage.createJoker(manhuntId, huntedUserId, jokerTypeId);

        return Response.ok(joker).build();
    }

    @Path("useJoker")
    @POST
    public Response useJoker(@QueryParam("jokerId") long jokerId, @QueryParam("deviceId") long deviceId) throws StorageException, TraccarException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var joker = manhuntDatabaseStorage.getJoker(jokerId);
        if(joker == null)
            throw new TraccarException("Der Joker konnte nicht gefunden werden.");

        if(joker.getJokerTypeId() == JokerType.SKIP_LOCATION) {
            var deviceDto = manhuntDatabaseStorage.getDevice(joker.getManhuntsId(), deviceId);
            CheckDevice(deviceDto);

            manhuntDatabaseStorage.setSkipNextLocation(deviceId);
        }

        manhuntDatabaseStorage.useJoker(joker);

        return Response.ok(joker).build();
    }

    @Path("refreshManhuntLocation")
    @POST
    public Response refreshManhuntLocation(@QueryParam("manhuntId") long manhuntId, @QueryParam("deviceId") long deviceId) throws StorageException, TraccarException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        permissionsService.checkRestriction(getUserId(), (userRestrictions) -> !userRestrictions.getTriggerManhuntActions());

        var manhunt = manhuntDatabaseStorage.getCurrent(true);

        var deviceDto = manhuntDatabaseStorage.getDevice(manhunt.getId(), deviceId);
        CheckDevice(deviceDto);

        var position = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(deviceId)));
        if(position == null)
            throw new TraccarException("Es konnte keine Position gefunden werden.");

        manhuntDatabaseStorage.saveManhuntPosition(position);
        connectionManager.updateAllPosition(true, position);

        return Response.ok(position).build();
    }

    @Path("assignGeofenceToAllUsers")
    @POST
    public Response assignGeofenceToAllUsers(@QueryParam("geofenceId") long geofenceId) throws Exception {
        permissionsService.checkPermission(Geofence.class, getUserId(), geofenceId);

        manhuntDatabaseStorage.assignGeofenceToAllUsers(geofenceId);

        var geofence = manhuntDatabaseStorage.getGeofence(geofenceId);
        connectionManager.sendAddGeofenceBroadcast(geofence);

        return Response.noContent().build();
    }

    @Path("removeGeofenceFromAllUsers")
    @POST
    public Response removeGeofenceFromAllUsers(@QueryParam("geofenceId") long geofenceId) throws Exception {
        permissionsService.checkPermission(Geofence.class, getUserId(), geofenceId);

        manhuntDatabaseStorage.removeGeofenceFromAllUsers(geofenceId);

        var geofence = manhuntDatabaseStorage.getGeofence(geofenceId);
        connectionManager.sendRemoveGeofenceBroadcast(geofence);

        return Response.noContent().build();
    }

    private void CheckDevice(DeviceDto dto) throws TraccarException {
        if(dto == null || dto.getId() == 0)
            throw new TraccarException("Das Gerät konnte nicht gefunden werden.");

        if(dto.getManhuntRole() != 2)
            throw new TraccarException("Das Gerät ist kein 'Gejagter'.");

        if(dto.getIsCaught())
            throw new TraccarException("Das Gerät wurde bereits verhaftet.");
    }

    private void sendSpeedHuntNotification(long numSpeedHunt, long maxSpeedHunts) throws StorageException {
        var notificationMessage = new NotificationMessage("Speedhunt", "Speedhunt " + numSpeedHunt + "/" + maxSpeedHunts + " gestartet!",
                "", true);
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }

    private void sendSpeedHuntRequestNotification(long numRequests, long maxRequests) throws StorageException {
        var notificationMessage = new NotificationMessage("Standortanfrage", "Standort " + numRequests + "/" + maxRequests + " angefragt!",
                "", true);
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }

    private void sendCatchNotification(Device device) throws StorageException {
        var notificationMessage = new NotificationMessage("Verhaftung", "Der Spieler '" + device.getName() + "' wurde verhaftet",
                "", true);
        connectionManager.sendNotificationToAllUsers(notificationMessage);
    }
}
