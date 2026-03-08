package org.traccar.schedule;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.*;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationMessage;
import org.traccar.notification.NotificatorManager;
import org.traccar.storage.ManhuntDatabaseStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskLocationUpdateCheck extends SingleScheduleTask{

    @Inject
    private NotificatorManager notificatorManager;

    @Inject
    private ManhuntDatabaseStorage manhuntDatabaseStorage;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskLocationUpdateCheck.class);

    private static final long CHECK_PERIOD_MINUTES = 2;

    private final Storage storage;

    @Inject
    public TaskLocationUpdateCheck(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long checkPeriod = TimeUnit.MINUTES.toMillis(CHECK_PERIOD_MINUTES);

        try {
            var currentManhunt = manhuntDatabaseStorage.getCurrent(false);
            if(currentManhunt == null || currentManhunt.getLocationUpdateReminderSeconds() <= 0)
                return;

            var interval = TimeUnit.SECONDS.toMillis(currentManhunt.getLocationUpdateReminderSeconds());
            var positions = storage.getObjects(Position.class, new Request(
                    new Columns.All(), new Condition.LatestPositions(0)));
            var users = manhuntDatabaseStorage.getAllUsers();
            for(Position position : positions){
                long lastTime = position.getFixTime().getTime();
                long diff = currentTime - lastTime;
                var locationUpdateIsExpired = (diff >= interval) && ((diff / interval) != ((diff - checkPeriod) / interval));
                if(locationUpdateIsExpired) {
                    Device device = storage.getObject(Device.class, new Request(
                            new Columns.All(), new Condition.Equals("id", position.getDeviceId())));
                    if(device == null || device.getManhuntUserId() <= 0)
                        continue;

                    User user = users.stream()
                            .filter(u -> u.getId() == device.getManhuntUserId())
                            .findFirst()
                            .orElse(null);
                    if(user == null)
                        continue;

                    ZoneId timezone = ZoneId.of("Europe/Berlin");
                    ZonedDateTime userTime = position.getFixTime().toInstant().atZone(timezone);
                    String readableTime = userTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    var notificationMessage = new NotificationMessage(
                            "Fehlende Standortupdates",
                            "Bitte prüfe den Traccar Client. Letztes Update: " + readableTime,
                            "",
                            true
                    );
                    sendNotification(user, notificationMessage);
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Database error", e);
        }
    }

    public void sendNotification(User user, NotificationMessage notificationMessage) {
        try {
            notificatorManager.getNotificator("traccar").send(user, notificationMessage, null, null);
        } catch (MessageException e) {
            LOGGER.warn("Notification error", e);
        }
    }
}
