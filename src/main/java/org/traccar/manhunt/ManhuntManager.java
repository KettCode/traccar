package org.traccar.manhunt;

import jakarta.inject.Inject;
import org.traccar.api.security.PermissionsService;
import org.traccar.model.Catches;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.ManhuntInfo;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.List;

public class ManhuntManager {

    @Inject
    protected Storage storage;

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    @Inject
    public ManhuntManager() {

    }

    public List<DeviceInfo> getDeviceInfos(long manhuntId) throws StorageException {
        var devices = manhuntDatabaseStorage.getHuntedDevices(manhuntId, true);
        var catches = storage.getObjects(Catches.class, new Request(new Columns.All(),
                new Condition.Equals("manhuntsId", manhuntId)));

        var deviceInfos = new ArrayList<DeviceInfo>();
        for(var device : devices) {
            var deviceInfo = new DeviceInfo();
            deviceInfo.setId(device.getId());
            deviceInfo.setName(device.getName());
            deviceInfo.setIsCaught(isCaught(catches, device.getId()));

            deviceInfos.add(deviceInfo);
        }

        return deviceInfos;
    }

    private boolean isCaught(List<Catches> catches, long deviceId) {
        return catches.stream().anyMatch(x -> x.getDeviceId() == deviceId);
    }
}

