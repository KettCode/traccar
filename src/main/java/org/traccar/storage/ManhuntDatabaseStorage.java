package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.helper.model.PositionUtil;
import org.traccar.manhunt.CreateCatchDto;
import org.traccar.manhunt.CreateSpeedHuntDto;
import org.traccar.manhunt.CreateSpeedHuntRequestDto;
import org.traccar.manhunt.DeviceInfo;
import org.traccar.model.*;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
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

    public List<DeviceInfo> getHuntedDevices(long manhuntId, boolean withCaught) throws StorageException {
        try {
            var query = "SELECT tc_devices.id, tc_devices.name, " +
                    "   CASE " +
                    "       WHEN tc_catches.id IS NULL THEN 0 " +
                    "       ELSE 1 " +
                    "   END AS isCaught " +
                    "FROM tc_devices " +
                    "JOIN tc_groups ON tc_groups.id = tc_devices.groupId " +
                    "LEFT JOIN tc_catches ON tc_catches.manhuntsId = :manhuntId and tc_catches.deviceId = tc_devices.id " +
                    "WHERE tc_groups.manhuntRole = 2 ";

            if(withCaught)
                query += "or tc_catches.id IS NOT NULL ";
            else
                query += "and tc_catches.id IS NULL ";

            query += "ORDER BY tc_devices.name ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntId", manhuntId);
            return builder.executeQuery(DeviceInfo.class);

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

    public CreateCatchDto getCreateCatchDto(long deviceId) throws StorageException {
        try {
            var query = "SELECT " +
                    "mh.id as manhuntId, " +
                    "d.id as deviceId, " +
                    "d.name as deviceName, " +
                    "g.manhuntRole as deviceManhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught " +
                    "FROM tc_manhunts mh " +
                    "LEFT JOIN tc_devices d on d.id = :deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = d.id and c.manhuntsId = mh.id " +
                    "LEFT JOIN tc_groups g on g.id = d.groupId " +
                    "WHERE mh.id = " +
                    "( " +
                    "   SELECT mh2.id " +
                    "   FROM tc_manhunts mh2 " +
                    "   ORDER BY mh2.start DESC LIMIT 1 " +
                    ") ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("deviceId", deviceId);
            var dto = builder.executeQuery(CreateCatchDto.class);
            return dto.isEmpty() ? null : dto.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public CreateSpeedHuntDto getCreateSpeedHuntDto() throws StorageException {
        try {
            var query = "SELECT " +
                    "mh.id as manhuntId, " +
                    "sh.id as speedHuntId, " +
                    "d.id as deviceId, " +
                    "d.name as deviceName, " +
                    "g.manhuntRole as manhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS isCaught, " +
                    "( " +
                    "   SELECT COUNT(sh3.id) " +
                    "   FROM tc_speedHunts sh3 " +
                    "   WHERE sh3.manhuntsId = mh.id " +
                    ") AS speedHunts, " +
                    "( " +
                    "   SELECT COUNT(shr.id) " +
                    "   FROM tc_speedHuntRequests shr " +
                    "   WHERE shr.speedHuntsId = sh.id " +
                    ") AS speedHuntRequests " +
                    "FROM tc_manhunts mh " +
                    "LEFT JOIN tc_speedHunts sh on sh.id = " +
                    "   ( " +
                    "       SELECT MAX(sh2.id) " +
                    "       FROM tc_speedHunts sh2 " +
                    "       WHERE sh2.manhuntsId = mh.id " +
                    "   ) " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = mh.id " +
                    "LEFT JOIN tc_groups g on g.id = d.groupId " +
                    "WHERE mh.id = " +
                    "( " +
                    "   SELECT mh2.id " +
                    "   FROM tc_manhunts mh2 " +
                    "   ORDER BY mh2.start DESC LIMIT 1 " +
                    ") ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            var dto = builder.executeQuery(CreateSpeedHuntDto.class);
            return dto.isEmpty() ? null : dto.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public CreateSpeedHuntRequestDto getCreateSpeedHuntRequestDto(long speedHuntId) throws StorageException {
        try {
            var query = "SELECT " +
                    "mh.id as manhuntId, " +
                    "sh.id as speedHuntId, " +
                    "d.id as deviceId, " +
                    "d.name as deviceName, " +
                    "g.manhuntRole as deviceManhuntRole, " +
                    "CASE " +
                    "   WHEN c.id IS NULL THEN 0 " +
                    "   ELSE 1 " +
                    "END AS deviceIsCaught, " +
                    "( " +
                    "   SELECT COUNT(shr.id) " +
                    "   FROM tc_speedHuntRequests shr " +
                    "   WHERE shr.speedHuntsId = sh.id " +
                    ") AS speedHuntRequests " +
                    "FROM tc_manhunts mh " +
                    "LEFT JOIN tc_speedHunts sh on sh.manhuntsId = mh.id and sh.id = :speedHuntId " +
                    "LEFT JOIN tc_devices d on d.id = sh.deviceId " +
                    "LEFT JOIN tc_catches c on c.deviceId = sh.deviceId and c.manhuntsId = mh.id " +
                    "LEFT JOIN tc_groups g on g.id = d.groupId " +
                    "WHERE mh.id = " +
                    "( " +
                    "   SELECT mh2.id " +
                    "   FROM tc_manhunts mh2 " +
                    "   ORDER BY mh2.start DESC LIMIT 1 " +
                    ") ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("speedHuntId", speedHuntId);
            var dto = builder.executeQuery(CreateSpeedHuntRequestDto.class);
            return dto.isEmpty() ? null : dto.get(0);
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

    public Group getGroupByDeviceId(long deviceId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_groups " +
                    "JOIN tc_devices ON tc_devices.groupId = tc_groups.id " +
                    "WHERE tc_devices.id = :deviceId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("deviceId", deviceId);
            var groups = builder.executeQuery(Group.class);
            return groups.isEmpty() ? null : groups.get(0);
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

    public ManhuntHunterInfo getManhuntHunterInfo(long userId) throws StorageException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return getManhuntInfo(userId, ManhuntHunterInfo.class);
    }

    public ManhuntHuntedInfo getManhuntHuntedInfo(long userId) throws StorageException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return getManhuntInfo(userId, ManhuntHuntedInfo.class);
    }

    public <T extends ManhuntInfo> T getManhuntInfo(long userId, Class<T> type) throws StorageException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var manhunt = getCurrent();
        if(manhunt == null)
            return type.getDeclaredConstructor().newInstance();

        var groups = storage.getObjects(Group.class, new Request(new Columns.All()));
        var devices = storage.getObjects(Device.class, new Request(new Columns.All()));
        var catches = storage.getObjects(Catches.class, new Request(new Columns.All(),
                new Condition.Equals("manhuntsId", manhunt.getId())));
        var group = getGroupByUserId(userId);
        var speedHunts = getSpeedHunts(manhunt.getId(), group);

        var manhuntInfo = type.getDeclaredConstructor().newInstance();
        manhuntInfo.setManhunt(manhunt);
        manhuntInfo.setSpeedHunts(speedHunts);
        manhuntInfo.setGroup(group);
        manhuntInfo.setCatches(catches);
        manhuntInfo.setGroups(groups);
        manhuntInfo.setDevices(devices);
        return manhuntInfo;
    }

    private List<SpeedHunt> getSpeedHunts(long manhuntId, Group group) throws StorageException {
        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Equals("manhuntsId", manhuntId));
        if(group != null && group.getManhuntRole() == 1)
            conditions.add(new Condition.Equals("hunterGroupId", group.getId()));

        var speedHunts = storage.getObjects(SpeedHunt.class, new Request(new Columns.All(), Condition.merge(conditions)));
        var speedHuntIds = speedHunts.stream().map(SpeedHunt::getId).toList();
        var speedHuntRequests = getSpeedHuntRequests(speedHuntIds);
        speedHunts.forEach(x -> {
            var speedHuntRequestsInternal = speedHuntRequests.stream()
                    .filter(y -> y.getSpeedHuntsId() == x.getId())
                    .toList();
            x.setSpeedHuntRequests(speedHuntRequestsInternal);
        });
        return speedHunts;
    }
}
