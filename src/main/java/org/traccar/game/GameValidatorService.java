package org.traccar.game;

import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;

import java.util.ArrayList;
import java.util.List;

public class GameValidatorService {

    public static final int MIN_PING_INTERVAL_SECONDS = 120;
    public static final int MIN_MAX_POSITION_AGE_SECONDS = 60;
    public static final int MIN_LOCATION_REMINDER_INTERVAL_SECONDS = 120;

    public void validateRole(String role) {
        if (!GameMember.ROLE_HUNTER.equals(role)
                && !GameMember.ROLE_HUNTED.equals(role)
                && !GameMember.ROLE_GAME_MANAGEMENT.equals(role)) {
            throw new IllegalArgumentException("Invalid game role");
        }
    }

    public void validateGeofenceType(String type) {
        if (!GameGeofence.TYPE_PLAYFIELD.equals(type)
                && !GameGeofence.TYPE_SAFE_ZONE.equals(type)
                && !GameGeofence.TYPE_RESTRICTED_ZONE.equals(type)
                && !GameGeofence.TYPE_EVENT_ZONE.equals(type)) {
            throw new IllegalArgumentException("Invalid game geofence type");
        }
    }

    public void validateSettings(Game game) {
        if (game.getPingIntervalSeconds() < MIN_PING_INTERVAL_SECONDS) {
            throw new IllegalArgumentException("Ping interval must be at least 120 seconds");
        }
        if (game.getMaxPositionAgeSeconds() < MIN_MAX_POSITION_AGE_SECONDS) {
            throw new IllegalArgumentException("Maximum position age must be at least 60 seconds");
        }
        if (game.getLocationReminderIntervalSeconds() < MIN_LOCATION_REMINDER_INTERVAL_SECONDS) {
            throw new IllegalArgumentException("Location reminder interval must be at least 120 seconds");
        }
        if (game.getSpeedhuntLimit() < 0) {
            throw new IllegalArgumentException("Speedhunt limit must not be negative");
        }
        if (game.getSpeedhuntPingLimit() < 0) {
            throw new IllegalArgumentException("Speedhunt ping limit must not be negative");
        }
        if (game.getSpeedhuntLimit() > 0 && game.getSpeedhuntPingLimit() == 0) {
            throw new IllegalArgumentException("Speedhunt ping limit must be positive when speedhunts are enabled");
        }
    }

    public List<String> getIssues(Game game, List<GameMember> members) {
        var issues = new ArrayList<String>();
        if (!Game.STATUS_DRAFT.equals(game.getStatus())) {
            issues.add("Game is not in draft status");
        }
        if (game.getPingIntervalSeconds() < MIN_PING_INTERVAL_SECONDS) {
            issues.add("Ping interval must be at least 120 seconds");
        }
        if (game.getMaxPositionAgeSeconds() < MIN_MAX_POSITION_AGE_SECONDS) {
            issues.add("Maximum position age must be at least 60 seconds");
        }
        if (game.getLocationReminderIntervalSeconds() < MIN_LOCATION_REMINDER_INTERVAL_SECONDS) {
            issues.add("Location reminder interval must be at least 120 seconds");
        }
        if (game.getSpeedhuntLimit() < 0) {
            issues.add("Speedhunt limit must not be negative");
        }
        if (game.getSpeedhuntPingLimit() < 0) {
            issues.add("Speedhunt ping limit must not be negative");
        }
        if (game.getSpeedhuntLimit() > 0 && game.getSpeedhuntPingLimit() == 0) {
            issues.add("Speedhunt ping limit must be positive when speedhunts are enabled");
        }

        return issues;
    }

}
