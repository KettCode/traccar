/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.session;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Protocol;
import org.traccar.broadcast.BroadcastInterface;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.DeviceLookupService;
import org.traccar.database.NotificationManager;
import org.traccar.model.*;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Singleton
public class ConnectionManager implements BroadcastInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final long deviceTimeout;
    private final boolean showUnknownDevices;

    private final Map<Long, DeviceSession> sessionsByDeviceId = new ConcurrentHashMap<>();
    private final Map<ConnectionKey, Map<String, DeviceSession>> sessionsByEndpoint = new ConcurrentHashMap<>();
    private final Map<ConnectionKey, String> unknownByEndpoint = new ConcurrentHashMap<>();

    private final Config config;
    private final CacheManager cacheManager;
    private final Storage storage;
    private final NotificationManager notificationManager;
    private final Timer timer;
    private final BroadcastService broadcastService;
    private final DeviceLookupService deviceLookupService;
    private final ManhuntDatabaseStorage manhuntDatabaseStorage;

    private final Map<Long, Set<UpdateListener>> listeners = new HashMap<>();
    private final Map<Long, Set<Long>> userDevices = new HashMap<>();
    private final Map<Long, Set<Long>> deviceUsers = new HashMap<>();
    private final Map<Long, Set<Long>> userHuntedDevices = new HashMap<>();

    private final Map<Long, Timeout> timeouts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> groupSchedules = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @Inject
    public ConnectionManager(
            Config config, CacheManager cacheManager, Storage storage,
            NotificationManager notificationManager, Timer timer, BroadcastService broadcastService,
            DeviceLookupService deviceLookupService, ManhuntDatabaseStorage manhuntDatabaseStorage) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.storage = storage;
        this.notificationManager = notificationManager;
        this.timer = timer;
        this.broadcastService = broadcastService;
        this.deviceLookupService = deviceLookupService;
        this.manhuntDatabaseStorage = manhuntDatabaseStorage;
        deviceTimeout = config.getLong(Keys.STATUS_TIMEOUT);
        showUnknownDevices = config.getBoolean(Keys.WEB_SHOW_UNKNOWN_DEVICES);
        broadcastService.registerListener(this);
        initSchedules();
    }

    public DeviceSession getDeviceSession(long deviceId) {
        return sessionsByDeviceId.get(deviceId);
    }

    public DeviceSession getDeviceSession(
            Protocol protocol, Channel channel, SocketAddress remoteAddress,
            String... uniqueIds) throws Exception {

        ConnectionKey connectionKey = new ConnectionKey(channel, remoteAddress);
        Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.getOrDefault(
                connectionKey, new ConcurrentHashMap<>());

        uniqueIds = Arrays.stream(uniqueIds).filter(Objects::nonNull).toArray(String[]::new);
        if (uniqueIds.length > 0) {
            for (String uniqueId : uniqueIds) {
                DeviceSession deviceSession = endpointSessions.get(uniqueId);
                if (deviceSession != null) {
                    return deviceSession;
                }
            }
        } else {
            return endpointSessions.values().stream().findAny().orElse(null);
        }

        Device device = deviceLookupService.lookup(uniqueIds);

        String firstUniqueId = uniqueIds[0];
        if (device == null && config.getBoolean(Keys.DATABASE_REGISTER_UNKNOWN)) {
            if (firstUniqueId.matches(config.getString(Keys.DATABASE_REGISTER_UNKNOWN_REGEX))) {
                device = addUnknownDevice(firstUniqueId);
            }
        }

        if (device != null) {
            unknownByEndpoint.remove(connectionKey);
            device.checkDisabled();

            DeviceSession oldSession = sessionsByDeviceId.remove(device.getId());
            if (oldSession != null) {
                Map<String, DeviceSession> oldEndpointSessions = sessionsByEndpoint.get(oldSession.getConnectionKey());
                if (oldEndpointSessions != null && oldEndpointSessions.size() > 1) {
                    oldEndpointSessions.remove(device.getUniqueId());
                } else {
                    sessionsByEndpoint.remove(oldSession.getConnectionKey());
                }
            }

            DeviceSession deviceSession = new DeviceSession(
                    device.getId(), device.getUniqueId(), device.getModel(), protocol, channel, remoteAddress);
            endpointSessions.put(device.getUniqueId(), deviceSession);
            sessionsByEndpoint.put(connectionKey, endpointSessions);
            sessionsByDeviceId.put(device.getId(), deviceSession);

            if (oldSession == null) {
                cacheManager.addDevice(device.getId(), connectionKey);
            }

            return deviceSession;
        } else {
            unknownByEndpoint.put(connectionKey, firstUniqueId);
            LOGGER.warn("Unknown device - " + String.join(" ", uniqueIds)
                    + " (" + ((InetSocketAddress) remoteAddress).getHostString() + ")");
            return null;
        }
    }

    private Device addUnknownDevice(String uniqueId) {
        Device device = new Device();
        device.setName(uniqueId);
        device.setUniqueId(uniqueId);
        device.setCategory(config.getString(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_CATEGORY));

        long defaultGroupId = config.getLong(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_GROUP_ID);
        if (defaultGroupId != 0) {
            device.setGroupId(defaultGroupId);
        }

        try {
            device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
            LOGGER.info("Automatically registered " + uniqueId);
            return device;
        } catch (StorageException e) {
            LOGGER.warn("Automatic registration failed", e);
            return null;
        }
    }

    public void deviceDisconnected(Channel channel, boolean supportsOffline) {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress != null) {
            ConnectionKey connectionKey = new ConnectionKey(channel, remoteAddress);
            Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.remove(connectionKey);
            if (endpointSessions != null) {
                for (DeviceSession deviceSession : endpointSessions.values()) {
                    if (supportsOffline) {
                        updateDevice(deviceSession.getDeviceId(), Device.STATUS_OFFLINE, null);
                    }
                    sessionsByDeviceId.remove(deviceSession.getDeviceId());
                    cacheManager.removeDevice(deviceSession.getDeviceId(), connectionKey);
                }
            }
            unknownByEndpoint.remove(connectionKey);
        }
    }

    public void deviceUnknown(long deviceId) {
        updateDevice(deviceId, Device.STATUS_UNKNOWN, null);
        removeDeviceSession(deviceId);
    }

    private void removeDeviceSession(long deviceId) {
        DeviceSession deviceSession = sessionsByDeviceId.remove(deviceId);
        if (deviceSession != null) {
            ConnectionKey connectionKey = deviceSession.getConnectionKey();
            cacheManager.removeDevice(deviceId, connectionKey);
            sessionsByEndpoint.computeIfPresent(connectionKey, (e, sessions) -> {
                sessions.remove(deviceSession.getUniqueId());
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    public void updateDevice(long deviceId, String status, Date time) {
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            try {
                device = storage.getObject(Device.class, new Request(
                        new Columns.All(), new Condition.Equals("id", deviceId)));
            } catch (StorageException e) {
                LOGGER.warn("Failed to get device", e);
            }
            if (device == null) {
                return;
            }
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);

        if (!status.equals(oldStatus)) {
            String eventType;
            Map<Event, Position> events = new HashMap<>();
            eventType = switch (status) {
                case Device.STATUS_ONLINE -> Event.TYPE_DEVICE_ONLINE;
                case Device.STATUS_UNKNOWN -> Event.TYPE_DEVICE_UNKNOWN;
                default -> Event.TYPE_DEVICE_OFFLINE;
            };
            events.put(new Event(eventType, deviceId), null);
            notificationManager.updateEvents(events);
        }

        if (time != null) {
            device.setLastUpdate(time);
        }

        Timeout timeout = timeouts.remove(deviceId);
        if (timeout != null) {
            timeout.cancel();
        }

        if (status.equals(Device.STATUS_ONLINE)) {
            timeouts.put(deviceId, timer.newTimeout(timeout1 -> {
                if (!timeout1.isCancelled()) {
                    deviceUnknown(deviceId);
                }
            }, deviceTimeout, TimeUnit.SECONDS));
        }

        try {
            storage.updateObject(device, new Request(
                    new Columns.Include("status", "lastUpdate"),
                    new Condition.Equals("id", deviceId)));
        } catch (StorageException e) {
            LOGGER.warn("Update device status error", e);
        }

        updateDevice(true, device);
    }

    public synchronized void sendKeepalive() {
        for (Set<UpdateListener> userListeners : listeners.values()) {
            for (UpdateListener listener : userListeners) {
                listener.onKeepalive();
            }
        }
    }

    @Override
    public synchronized void updateDevice(boolean local, Device device) {
        if (local) {
            broadcastService.updateDevice(true, device);
        } else if (Device.STATUS_ONLINE.equals(device.getStatus())) {
            timeouts.remove(device.getId());
            removeDeviceSession(device.getId());
        }
        for (long userId : deviceUsers.getOrDefault(device.getId(), Collections.emptySet())) {
            if (listeners.containsKey(userId)) {
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdateDevice(device);
                }
            }
        }
    }

    @Override
    public synchronized void updatePosition(boolean local, Position position) {
        if (local) {
            broadcastService.updatePosition(true, position);
        }

        for (long userId : deviceUsers.getOrDefault(position.getDeviceId(), Collections.emptySet())) {
            if (listeners.containsKey(userId)) {
                if(userHuntedDevices.containsKey(userId)){
                    var huntedDeviceIds = userHuntedDevices.get(userId);
                    if(huntedDeviceIds.contains(position.getDeviceId()))
                        continue;
                }
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    public synchronized void updateHunterPosition(boolean local, Position position, List<Long> userIds) {
        if (local) {
            broadcastService.updatePosition(true, position);
        }

        for (long userId : deviceUsers.getOrDefault(position.getDeviceId(), Collections.emptySet())) {
            if (listeners.containsKey(userId)) {
                if(!userIds.contains(userId))
                    continue;
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    public synchronized void updateAllPosition(boolean local, Position position) {
        if (local) {
            broadcastService.updatePosition(true, position);
        }

        for (long userId : deviceUsers.getOrDefault(position.getDeviceId(), Collections.emptySet())) {
            if (listeners.containsKey(userId)) {
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    @Override
    public synchronized void updateEvent(boolean local, long userId, Event event) {
        if (local) {
            broadcastService.updateEvent(true, userId, event);
        }
        if (listeners.containsKey(userId)) {
            for (UpdateListener listener : listeners.get(userId)) {
                listener.onUpdateEvent(event);
            }
        }
    }

    @Override
    public synchronized <T1 extends BaseModel, T2 extends BaseModel> void invalidatePermission(
            boolean local, Class<T1> clazz1, long id1, Class<T2> clazz2, long id2, boolean link) {
        if (link && clazz1.equals(User.class) && clazz2.equals(Device.class)) {
            if (listeners.containsKey(id1)) {
                userDevices.get(id1).add(id2);
                deviceUsers.put(id2, new HashSet<>(List.of(id1)));
            }
        }
    }

    public synchronized void updateLog(LogRecord record) {
        var sessions = sessionsByEndpoint.getOrDefault(record.getConnectionKey(), Map.of());
        if (sessions.isEmpty()) {
            String unknownUniqueId = unknownByEndpoint.get(record.getConnectionKey());
            if (unknownUniqueId != null && showUnknownDevices) {
                record.setUniqueId(unknownUniqueId);
                listeners.values().stream()
                        .flatMap(Set::stream)
                        .forEach((listener) -> listener.onUpdateLog(record));
            }
        } else {
            var firstEntry = sessions.entrySet().iterator().next();
            record.setUniqueId(firstEntry.getKey());
            record.setDeviceId(firstEntry.getValue().getDeviceId());
            for (long userId : deviceUsers.getOrDefault(record.getDeviceId(), Set.of())) {
                for (UpdateListener listener : listeners.getOrDefault(userId, Set.of())) {
                    listener.onUpdateLog(record);
                }
            }
        }
    }

    public interface UpdateListener {
        void onKeepalive();
        void onUpdateDevice(Device device);
        void onUpdatePosition(Position position);
        void onUpdateEvent(Event event);
        void onUpdateLog(LogRecord record);
    }

    public synchronized void addListener(long userId, UpdateListener listener) throws StorageException {
        var set = listeners.get(userId);
        if (set == null) {
            set = new HashSet<>();
            listeners.put(userId, set);

            var devices = storage.getObjects(Device.class, new Request(
                    new Columns.Include("id"), new Condition.Permission(User.class, userId, Device.class)));
            userDevices.put(userId, devices.stream().map(BaseModel::getId).collect(Collectors.toSet()));
            devices.forEach(device -> deviceUsers.computeIfAbsent(device.getId(), id -> new HashSet<>()).add(userId));

            var user = manhuntDatabaseStorage.getHunterGroup(userId);
            if(user != null) {
                var huntedDevices = manhuntDatabaseStorage.getHuntedDevices(devices);
                userHuntedDevices.put(userId, huntedDevices.stream().map(BaseModel::getId).collect(Collectors.toSet()));
            }
        }
        set.add(listener);
    }

    public synchronized void removeListener(long userId, UpdateListener listener) {
        var set = listeners.get(userId);
        set.remove(listener);
        if (set.isEmpty()) {
            listeners.remove(userId);

            userDevices.remove(userId).forEach(deviceId -> deviceUsers.computeIfPresent(deviceId, (x, userIds) -> {
                userIds.remove(userId);
                return userIds.isEmpty() ? null : userIds;
            }));

            userHuntedDevices.remove(userId);
        }
    }

    public void initSchedules() {
        try {
            var groups = manhuntDatabaseStorage.getGroups(2);
            for(var group : groups) {
                scheduleUpdates(group);
            }
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    public void scheduleUpdates(Group group) throws StorageException {
        cancelScheduler(group.getId());

        var manhunt = manhuntDatabaseStorage.getCurrent();
        var frequency = group.getFrequency();
        if(frequency <= 0)
            frequency = 3600;
        var start = manhunt.getStart();

        var now = new Date();
        var initialDelay = 0L;
        if(now.before(start)){
            initialDelay = Duration.between(now.toInstant(), start.toInstant()).getSeconds();
        }  else {
            var durationBetween = Duration.between(start.toInstant(), now.toInstant()).getSeconds();
            var remainder = durationBetween % frequency;
            initialDelay = remainder == 0 ? 0 : (frequency - remainder);
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                var deviceIds = manhuntDatabaseStorage.getDevices(group.getId())
                        .stream().map(Device::getId)
                        .toList();

                var positions = storage.getObjects(Position.class, new Request(
                        new Columns.All(), new Condition.LatestPositions()))
                        .stream().filter(x -> deviceIds.contains(x.getDeviceId()))
                        .toList();

                for(var position : positions) {
                    updateAllPosition(true, position);
                }
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }, initialDelay, frequency, TimeUnit.SECONDS);

        groupSchedules.put(group.getId(), future);
    }

    public void cancelScheduler(long groupId) {
        if(!groupSchedules.containsKey(groupId))
            return;

        var future = groupSchedules.get(groupId);
        if(future != null && !future.isCancelled()) {
            future.cancel(false);
        }
        groupSchedules.remove(groupId);
    }

}
