package org.traccar.game;

import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;

import java.util.ArrayList;
import java.util.List;

public class GameValidatorService {

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
        if (game.getPingIntervalSeconds() <= 0) {
            throw new IllegalArgumentException("Ping interval must be positive");
        }
        if (game.getMaxPositionAgeSeconds() <= 0) {
            throw new IllegalArgumentException("Maximum position age must be positive");
        }
        if (game.getLocationReminderIntervalSeconds() <= 0) {
            throw new IllegalArgumentException("Location reminder interval must be positive");
        }
        if (game.getFakePingMaxDistanceMeters() <= 0) {
            throw new IllegalArgumentException("Fake ping maximum distance must be positive");
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
        if (game.getPingIntervalSeconds() <= 0) {
            issues.add("Ping interval must be positive");
        }
        if (game.getMaxPositionAgeSeconds() <= 0) {
            issues.add("Maximum position age must be positive");
        }
        if (game.getLocationReminderIntervalSeconds() <= 0) {
            issues.add("Location reminder interval must be positive");
        }
        if (game.getFakePingMaxDistanceMeters() <= 0) {
            issues.add("Fake ping maximum distance must be positive");
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

        boolean hasHunter = false;
        boolean hasHunted = false;
        for (GameMember member : members) {
            if (GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
                hasHunter |= GameMember.ROLE_HUNTER.equals(member.getRole());
                hasHunted |= GameMember.ROLE_HUNTED.equals(member.getRole());
            }
        }
        if (!hasHunter) {
            issues.add("No active hunter configured");
        }
        if (!hasHunted) {
            issues.add("No active hunted player configured");
        }
        return issues;
    }

}
