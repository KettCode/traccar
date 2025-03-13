package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.helper.model.PositionUtil;
import org.traccar.manhunt.dto.DeviceDto;
import org.traccar.manhunt.dto.SpeedHuntDto;
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

    public List<Device> getHuntedDevices(List<Device> devices) throws StorageException {
        try {
            var ids = devices.stream().map(Device::getId).toArray();
            var query = "SELECT * " +
                    "FROM tc_devices " +
                    "JOIN tc_groups ON tc_groups.id = tc_devices.groupId " +
                    "WHERE tc_devices.id = ANY(:deviceIds) and tc_groups.manhuntRole = 2 ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setArray("deviceIds", ids, true);
            return builder.executeQuery(Device.class);

        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<Device> getDevices(long manhuntRole) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_devices " +
                    "JOIN tc_groups ON tc_groups.id = tc_devices.groupId " +
                    "WHERE tc_groups.manhuntRole = :manhuntRole ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntRole", manhuntRole);
            return builder.executeQuery(Device.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<SpeedHuntRequest> getSpeedHuntRequests(List<Long> speedHuntIds) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_speedHuntRequests " +
                    "WHERE speedHuntsId = ANY(:speedHuntIds) " +
                    "ORDER BY speedHuntsId, time";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setArray("speedHuntIds", speedHuntIds.toArray(), true);
            return builder.executeQuery(SpeedHuntRequest.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public Group getGroupByUserId(long userId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_groups " +
                    "JOIN tc_users ON tc_users.groupId = tc_groups.id " +
                    "WHERE tc_users.id = :userId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("userId", userId);
            var groups = builder.executeQuery(Group.class);
            return groups.isEmpty() ? new Group() : groups.get(0);
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
                    "g.manhuntRole as manhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught " +
                    "FROM tc_devices d " +
                    "LEFT JOIN tc_catches c on c.deviceId = d.id and c.manhuntsId = :manhuntId " +
                    "LEFT JOIN tc_groups g on g.id = d.groupId " +
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

    public List<DeviceDto> getHuntedDevices(long manhuntId, boolean withCaught) throws StorageException {
        try {
            var query = "SELECT d.*, " +
                    "g.manhuntRole as manhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught " +
                    "FROM tc_devices d " +
                    "JOIN tc_groups g ON g.id = d.groupId " +
                    "LEFT JOIN tc_catches c ON c.manhuntsId = :manhuntId and c.deviceId = d.id " +
                    "WHERE g.manhuntRole = 2 ";

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

    public SpeedHuntDto getLastSpeedHunt(long manhuntId) throws StorageException {
        try {
            var query = "SELECT sh.*, " +
                    "d.name as deviceName, " +
                    "hunted_g.id as deviceGroupId, " +
                    "hunted_g.manhuntRole as deviceManhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught, " +
                    "hunter_g.speedHuntRequests as maxRequests, " +
                    "( " +
                    "   SELECT COUNT(shr.id) " +
                    "   FROM tc_speedHuntRequests shr " +
                    "   WHERE shr.speedHuntsId = sh.id " +
                    ") AS numRequests " +
                    "FROM tc_speedHunts sh " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = sh.manhuntsId " +
                    "LEFT JOIN tc_groups hunted_g on hunted_g.id = d.groupId " +
                    "LEFT JOIN tc_groups hunter_g on hunter_g.id = sh.hunterGroupId " +
                    "WHERE sh.manhuntsId = :manhuntId " +
                    "and sh.id = " +
                    "( " +
                    "   SELECT MAX(sh2.id) " +
                    "   FROM tc_speedHunts sh2 " +
                    "   WHERE sh2.manhuntsId = sh.manhuntsId " +
                    ") ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            var dto = builder.executeQuery(SpeedHuntDto.class);
            return dto.isEmpty() ? null : dto.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<SpeedHuntDto> getSpeedHunts(long manhuntId) throws StorageException {
        try {
            var query = "SELECT sh.*, " +
                    "d.name as deviceName, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught, " +
                    "hunter_g.speedHuntRequests as maxRequests " +
                    "FROM tc_speedHunts sh " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = :manhuntId " +
                    "LEFT JOIN tc_groups hunter_g on hunter_g.id = sh.hunterGroupId " +
                    "WHERE sh.manhuntsId = :manhuntId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            return builder.executeQuery(SpeedHuntDto.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<Position> getManhuntPositions(long userId) throws StorageException {

        var manhunt = getCurrent();
        var group = getGroupByUserId(userId);

        if(manhunt == null || group == null)
            return PositionUtil.getLatestPositions(storage, userId);

        if (group.getManhuntRole() == 1) {
            return PositionUtil.getHunterPositions(storage, userId);
        } else if (group.getManhuntRole() == 2) {
            return PositionUtil.getHuntedPositions(storage, userId);
        } else
            return PositionUtil.getLatestPositions(storage, userId);
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

    public Catches createCatch(long manhuntId, long groupId, long deviceId) throws StorageException {
        var catch1 = new Catches();
        catch1.setManhuntsId(manhuntId);
        catch1.setHunterGroupId(groupId);
        catch1.setDeviceId(deviceId);
        catch1.setTime(new Date());
        catch1.setId(storage.addObject(catch1, new Request(new Columns.Exclude("id"))));
        return catch1;
    }

    public SpeedHunt createSpeedHunt(long manhuntId, long groupId, long deviceId) throws StorageException {
        var speedHunt = new SpeedHunt();
        speedHunt.setManhuntsId(manhuntId);
        speedHunt.setHunterGroupId(groupId);
        speedHunt.setDeviceId(deviceId);
        speedHunt.setId(storage.addObject(speedHunt, new Request(new Columns.Exclude("id"))));
        return speedHunt;
    }

    public SpeedHuntRequest createSpeedHuntRequest(long speedHuntId, long userId) throws StorageException {
        var speedHuntRequest = new SpeedHuntRequest();
        speedHuntRequest.setSpeedHuntsId(speedHuntId);
        speedHuntRequest.setUserId(userId);
        speedHuntRequest.setTime(new Date());
        speedHuntRequest.setId(storage.addObject(speedHuntRequest, new Request(new Columns.Exclude("id"))));
        return speedHuntRequest;
    }
}
