package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;
import org.traccar.config.Config;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.model.*;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ManhuntDatabaseStorage {

    private final Config config;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    private Storage storage;

    @Inject
    public ManhuntDatabaseStorage(Config config, DataSource dataSource, ObjectMapper objectMapper) {
        this.config = config;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public Manhunt getCurrent(boolean loadCascade) throws StorageException {
        var manhunt = storage.getObject(Manhunt.class, new Request(
                new Columns.All(),
                new Order("start", true, 1)
        ));

        if(loadCascade && manhunt != null) {
            var speedHunts = getSpeedHunts(manhunt.getId(), true);
            manhunt.setSpeedHunts(speedHunts);
        }

        return manhunt;
    }

    public List<SpeedHunt> getSpeedHunts(@QueryParam("manhuntId") long manhuntId, @QueryParam("withLocationRequests") boolean withLocationRequests) throws StorageException {
        var speedHunts = storage.getObjects(SpeedHunt.class, new Request(
                new Columns.All(),
                new Condition.Equals("manhuntsId", manhuntId),
                new Order("id")
        ));

        if(speedHunts == null)
            return new ArrayList<>();

        if(withLocationRequests) {
            var speedHuntIds = speedHunts.stream().map(SpeedHunt::getId).toList();
            var locationRequests = getSpeedHuntRequests(speedHuntIds);

            var locationRequestMap = locationRequests.stream()
                    .collect(Collectors.groupingBy(LocationRequest::getSpeedHuntsId));

            speedHunts.forEach(speedHunt -> {
                var locationRequestsForHunt = locationRequestMap.getOrDefault(speedHunt.getId(), Collections.emptyList());
                speedHunt.setLocationRequests(locationRequestsForHunt);
            });
        }

        return speedHunts;
    }

    public List<LocationRequest> getSpeedHuntRequests(List<Long> speedHuntIds)
            throws StorageException {

        if (speedHuntIds == null || speedHuntIds.isEmpty()) {
            return List.of();
        }

        try {
            String placeholders = speedHuntIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));

            String query = "SELECT * " +
                    "FROM tc_locationRequests " +
                    "WHERE speedHuntsId IN (" + placeholders + ") " +
                    "ORDER BY speedHuntsId, time";

            QueryBuilder builder = QueryBuilder.create(
                    config, dataSource, objectMapper, query);

            int index = 0;
            for (Long id : speedHuntIds) {
                builder.setLong(index++, id);
            }

            return builder.executeQuery(LocationRequest.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<User> getAllUsers() throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_users ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            return builder.executeQuery(User.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public DeviceDto getDevice(long manhuntId, long deviceId) throws StorageException {
        try {
            var query = "SELECT d.*, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught " +
                    "FROM tc_devices d " +
                    "LEFT JOIN tc_catches c on c.deviceId = d.id and c.manhuntsId = ? " +
                    "WHERE d.id = ? ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong(0, manhuntId);
            builder.setLong(1, deviceId);
            var dto = builder.executeQuery(DeviceDto.class);
            return dto.isEmpty() ? null : dto.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<DeviceDto> getDevices(long manhuntId) throws StorageException {
        try {
            var query = "SELECT d.*, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught " +
                    "FROM tc_devices d " +
                    "LEFT JOIN tc_catches c ON c.manhuntsId = ? and c.deviceId = d.id " +
                    "ORDER BY d.name";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong(0, manhuntId);
            return builder.executeQuery(DeviceDto.class);

        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public void saveManhuntPosition(Position position) throws StorageException {
        Device device = new Device();
        device.setId(position.getDeviceId());
        device.setManhuntPositionId(position.getId());
        storage.updateObject(device, new Request(
                new Columns.Include("manhuntPositionId"),
                new Condition.Equals("id", device.getId())));

        position.setIsManhunt(true);
        storage.updateObject(position, new Request(
                new Columns.Include("isManhunt"),
                new Condition.Equals("id", position.getId())
        ));
    }

    public Catches createCatch(long manhuntId, long userId, long deviceId) throws StorageException {
        var catch1 = new Catches();
        catch1.setManhuntsId(manhuntId);
        catch1.setUserId(userId);
        catch1.setDeviceId(deviceId);
        catch1.setTime(new Date());
        catch1.setId(storage.addObject(catch1, new Request(new Columns.Exclude("id"))));
        return catch1;
    }

    public SpeedHunt createSpeedHunt(long manhuntId, long userId, long deviceId) throws StorageException {
        var speedHunt = new SpeedHunt();
        speedHunt.setManhuntsId(manhuntId);
        speedHunt.setUserId(userId);
        speedHunt.setDeviceId(deviceId);
        speedHunt.setId(storage.addObject(speedHunt, new Request(new Columns.Exclude("id"))));
        return speedHunt;
    }

    public LocationRequest createLocationRequest(long speedHuntId, long userId) throws StorageException {
        var locationRequest = new LocationRequest();
        locationRequest.setSpeedHuntsId(speedHuntId);
        locationRequest.setUserId(userId);
        locationRequest.setTime(new Date());
        locationRequest.setId(storage.addObject(locationRequest, new Request(new Columns.Exclude("id"))));
        return locationRequest;
    }
}
