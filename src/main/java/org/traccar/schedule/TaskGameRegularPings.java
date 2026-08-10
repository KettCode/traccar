package org.traccar.schedule;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.game.ping.GameRegularPingService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskGameRegularPings implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskGameRegularPings.class);

    private static final long PERIOD_SECONDS = 10;

    private final GameRegularPingService regularPingService;

    @Inject
    public TaskGameRegularPings(GameRegularPingService regularPingService) {
        this.regularPingService = regularPingService;
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
            regularPingService.runDuePings();
        } catch (Exception e) {
            LOGGER.warn("Game regular ping error", e);
        }
    }

}
