package org.traccar.schedule;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.game.location.GameLocationReminderService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskGameLocationReminders implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskGameLocationReminders.class);

    private static final long PERIOD_SECONDS = 30;

    private final GameLocationReminderService locationReminderService;

    @Inject
    public TaskGameLocationReminders(GameLocationReminderService locationReminderService) {
        this.locationReminderService = locationReminderService;
    }

    @Override
    public boolean multipleInstances() {
        return false;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, PERIOD_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            locationReminderService.runDueReminders();
        } catch (Exception e) {
            LOGGER.warn("Game location reminder error", e);
        }
    }

}
