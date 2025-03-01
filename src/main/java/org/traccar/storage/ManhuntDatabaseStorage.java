package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.*;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
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

    public List<Device> getHuntedDevices(long userId, boolean isAdmin) throws StorageException {
        try {
            var conditions = new LinkedList<Condition>();

            if (!isAdmin) {
                conditions.add(new Condition.Permission(User.class, userId, Device.class));
            }

            var manhunt = getCurrent();

            var devices = storage.getObjects(Device.class, new Request(
                    new Columns.All(), Condition.merge(conditions), new Order("name")));

            var ids = devices.stream().map(Device::getId).toArray();
            var query = "SELECT * " +
                    "FROM tc_devices " +
                    "JOIN tc_groups ON tc_groups.id = tc_devices.groupId " +
                    "LEFT JOIN tc_catches ON tc_catches.manhuntsId = :manhuntId and tc_catches.deviceId = tc_devices.id " +
                    "WHERE tc_devices.id = ANY(:deviceIds) and tc_groups.manhuntRole = 2 and tc_catches.id IS NULL ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setArray("deviceIds", ids, true);
            builder.setLong("manhuntId", manhunt.getId());
            return builder.executeQuery(Device.class);

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

    public List<Device> getDevices(long groupId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_devices " +
                    "JOIN tc_groups ON tc_groups.id = tc_devices.groupId " +
                    "WHERE tc_groups.id = :groupId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("groupId", groupId);
            return builder.executeQuery(Device.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<SpeedHunt> getSpeedHunts(long groupId, long manhuntId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_speedHunts " +
                    "WHERE tc_speedHunts.manhuntsId = :manhuntsId " +
                    "AND tc_speedHunts.hunterGroupId = :groupId " +
                    "ORDER BY tc_speedHunts.pos";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntsId", manhuntId);
            builder.setLong("groupId", groupId);
            return builder.executeQuery(SpeedHunt.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<SpeedHuntRequest> getSpeedHuntRequests(List<Long> speedHuntIds) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_speedHuntRequests " +
                    "WHERE speedHuntsId = ANY(:speedHuntIds) " +
                    "ORDER BY speedHuntsId, pos";

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
                    "WHERE tc_devices.id = :deviceId and tc_groups.manhuntRole = 2 ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("deviceId", deviceId);
            var groups = builder.executeQuery(Group.class);
            return groups.isEmpty() ? null : groups.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<Group> getGroups(long manhuntRole) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_groups " +
                    "WHERE tc_groups.manhuntRole = :manhuntRole ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntRole", manhuntRole);
            return builder.executeQuery(Group.class);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<User> getUsers(long groupId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_users " +
                    "JOIN tc_groups ON tc_groups.id = tc_users.groupId " +
                    "WHERE tc_groups.id = :groupId ";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("groupId", groupId);
            return builder.executeQuery(User.class);
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

        if(manhunt == null || group == null || group.getManhuntRole() != 1)
            return PositionUtil.getLatestPositions(storage, userId);

        return PositionUtil.getManhuntPositions(storage, userId, manhunt.getStart(), manhunt.getId(), group.getId());
    }

    public ManhuntInfo getManhuntInfo(long userId) throws StorageException {
        var manhunt = getCurrent();
        if(manhunt == null)
            return new ManhuntInfo();

        var catches = storage.getObjects(Catches.class, new Request(new Columns.All(),
                new Condition.Equals("manhuntsId", manhunt.getId())));
        manhunt.setCatches(catches);

        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Equals("manhuntsId", manhunt.getId()));
        var group = getGroupByUserId(userId);
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

        var speedHuntInfo = new ManhuntInfo();
        speedHuntInfo.setManhunt(manhunt);
        speedHuntInfo.setSpeedHunts(speedHunts);
        speedHuntInfo.setGroup(group);
        speedHuntInfo.setCatches(catches);
        return speedHuntInfo;
    }
}
