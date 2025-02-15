package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.model.Manhunt;
import org.traccar.model.SpeedHunt;
import org.traccar.model.SpeedHuntRequest;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
            var query = "SELECT * FROM tc_manhunts WHERE start < CURRENT_TIMESTAMP ORDER BY start DESC LIMIT 1";
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            var manhunts = builder.executeQuery(Manhunt.class);
            return manhunts.isEmpty() ? null : manhunts.get(0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public List<SpeedHunt> getSpeedHunts(long userId, long manhuntId) throws StorageException {
        try {
            var query = "SELECT * " +
                    "FROM tc_speedHunts " +
                    "JOIN tc_users on tc_users.groupId = tc_speedHunts.hunterGroupId " +
                    "WHERE manhuntsId = :manhuntsId " +
                    "AND tc_users.id = :userId " +
                    "ORDER BY tc_speedHunts.pos";

            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query);
            builder.setLong("manhuntsId", manhuntId);
            builder.setLong("userId", userId);
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
}
