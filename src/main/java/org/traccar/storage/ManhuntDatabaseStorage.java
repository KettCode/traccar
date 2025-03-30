package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.manhunt.dto.LastSpeedHuntDto;
import org.traccar.model.*;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

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

    public Manhunt getCurrent() throws StorageException {
        try {
            var query = "SELECT * FROM tc_manhunts ORDER BY start DESC LIMIT 1";
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            var manhunts = builder.executeQuery(Manhunt.class);
            return manhunts.isEmpty() ? null : manhunts.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<LocationRequest> getSpeedHuntRequests(List<Long> speedHuntIds) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_locationRequests " +
                    "WHERE speedHuntsId = ANY(:speedHuntIds) " +
                    "ORDER BY speedHuntsId, time";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setArray("speedHuntIds", speedHuntIds.toArray(), true);
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
                    "LEFT JOIN tc_catches c on c.deviceId = d.id and c.manhuntsId = :manhuntId " +
                    "WHERE d.id = :deviceId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            builder.setLong("deviceId", deviceId);
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
                    "LEFT JOIN tc_catches c ON c.manhuntsId = :manhuntId and c.deviceId = d.id " +
                    "ORDER BY d.name";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            return builder.executeQuery(DeviceDto.class);

        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<DeviceDto> getHuntedDevices(long manhuntId, boolean withCaught) throws StorageException {
        try {
            var query = "SELECT d.*, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught " +
                    "FROM tc_devices d " +
                    "LEFT JOIN tc_catches c ON c.manhuntsId = :manhuntId and c.deviceId = d.id " +
                    "WHERE d.manhuntRole = 2 ";

            if(withCaught)
                query += "or c.id IS NOT NULL ";
            else
                query += "and c.id IS NULL ";

            query += "ORDER BY d.name ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            return builder.executeQuery(DeviceDto.class);

        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public LastSpeedHuntDto getLastSpeedHunt(long manhuntId) throws StorageException {
        try {
            var query = "SELECT sh.*, " +
                    "d.name as deviceName, " +
                    "d.manhuntRole as deviceManhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught, " +
                    "( " +
                    "   SELECT COUNT(shr.id) " +
                    "   FROM tc_locationRequests shr " +
                    "   WHERE shr.speedHuntsId = sh.id " +
                    ") AS numRequests " +
                    "FROM tc_speedHunts sh " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = sh.manhuntsId " +
                    "WHERE sh.manhuntsId = :manhuntId " +
                    "ORDER BY sh.id DESC " +
                    "LIMIT 1";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            var dto = builder.executeQuery(LastSpeedHuntDto.class);
            return dto.isEmpty() ? null : dto.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<LastSpeedHuntDto> getSpeedHunts(long manhuntId) throws StorageException {
        try {
            var query = "SELECT sh.*, " +
                    "d.name as deviceName, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught " +
                    "FROM tc_speedHunts sh " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = :manhuntId " +
                    "WHERE sh.manhuntsId = :manhuntId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            return builder.executeQuery(LastSpeedHuntDto.class);
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
