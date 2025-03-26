package org.traccar.service;

import jakarta.inject.Inject;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

public class PositionService {
    @Inject
    private Storage storage;

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    public PositionService() {

    }

    public List<Position> getLatestPositions(long userId) throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        var user = storage.getObject(User.class,
                new Request(new Columns.All(), new Condition.Equals("id", userId)));

        if (manhunt != null && user.getManhuntRole() == 1) {
            return PositionUtil.getLatestPositionsForHunter(storage, userId);
        } else if (manhunt != null && user.getManhuntRole() == 2) {
            return PositionUtil.getLatestPositionsForHunted(storage, userId);
        } else
            return PositionUtil.getLatestPositions(storage, userId);
    }

    public List<Position> getLatestPositions(long userId, long deviceId) throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        var user = storage.getObject(User.class,
                new Request(new Columns.All(), new Condition.Equals("id", userId)));

        if (manhunt != null && user.getManhuntRole() == 1) {
            return storage.getObjects(Position.class, new Request(
                    new Columns.All(), new Condition.LatestPositionsForHunter(deviceId)));
        } else if (manhunt != null && user.getManhuntRole() == 2) {
            return storage.getObjects(Position.class, new Request(
                    new Columns.All(), new Condition.LatestPositionsForHunted(deviceId)));
        } else
            return storage.getObjects(Position.class, new Request(
                    new Columns.All(), new Condition.LatestPositions(deviceId)));
    }

    public List<Position> getPositions(long userId, long deviceId, Date from, Date to) throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        var user = storage.getObject(User.class,
                new Request(new Columns.All(), new Condition.Equals("id", userId)));
        var device = storage.getObject(Device.class,
                new Request(new Columns.All(), new Condition.Equals("id", deviceId)));

        var positions = PositionUtil.getPositions(storage, deviceId, from, to);
        if (manhunt != null && user.getManhuntRole() == 1 && device.getManhuntRole() == 2) {
            return positions.stream().filter(Position::getIsManhunt).toList();
        }

        return positions;
    }

    public List<Position> getPositions(long userId, Device device, Date from, Date to) throws StorageException {
        var manhunt = manhuntDatabaseStorage.getCurrent();
        var user = storage.getObject(User.class,
                new Request(new Columns.All(), new Condition.Equals("id", userId)));

        var positions = PositionUtil.getPositions(storage, device.getId(), from, to);
        if (manhunt != null && user.getManhuntRole() == 1 && device.getManhuntRole() == 2) {
            return positions.stream().filter(Position::getIsManhunt).toList();
        }

        return positions;
    }
}
