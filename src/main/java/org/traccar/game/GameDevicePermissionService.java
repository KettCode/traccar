package org.traccar.game;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.model.Device;
import org.traccar.model.GameMember;
import org.traccar.model.Player;
import org.traccar.storage.StorageException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GameDevicePermissionService {

    private record GameParticipant(long userId, long deviceId, String role) {}

    @Inject
    private GamePermissionService gamePermissionService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private GameStorage gameStorage;

    public void validateActiveParticipants(long gameId) throws StorageException {
        List<GameParticipant> participants = getActiveParticipants(gameId);
        boolean hasHunter = false;
        boolean hasHunted = false;
        for (GameParticipant participant : participants) {
            hasHunter |= GameMember.ROLE_HUNTER.equals(participant.role());
            hasHunted |= GameMember.ROLE_HUNTED.equals(participant.role());
        }
        if (!hasHunter) {
            throw new IllegalArgumentException("No active hunter configured");
        }
        if (!hasHunted) {
            throw new IllegalArgumentException("No active hunted player configured");
        }
    }

    public void syncGameDevicePermissions(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        List<GameParticipant> participants = getActiveParticipants(gameId);
        clearGameDevicePermissions(userId, getNonLeftParticipants(gameId), httpRequest);

        var added = new HashSet<String>();
        for (GameParticipant participant : participants) {
            for (GameParticipant target : participants) {
                if (canSeeDevice(participant.role(), target.role())) {
                    String key = participant.userId() + ":" + target.deviceId();
                    if (added.add(key)) {
                        gamePermissionService.addPermission(
                                httpRequest, userId, participant.userId(), Device.class, target.deviceId());
                    }
                }
            }
        }
    }

    public void clearGameDevicePermissions(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        clearGameDevicePermissions(userId, getNonLeftParticipants(gameId), httpRequest);
    }

    private List<GameParticipant> getActiveParticipants(long gameId) throws StorageException {
        var participants = new ArrayList<GameParticipant>();
        for (GameMember member : gameStorage.getActiveGameMembers(gameId)) {
            participants.add(toParticipant(member));
        }
        return participants;
    }

    private List<GameParticipant> getNonLeftParticipants(long gameId) throws StorageException {
        var participants = new ArrayList<GameParticipant>();
        for (GameMember member : gameStorage.getNonLeftGameMembers(gameId)) {
            participants.add(toParticipant(member));
        }
        return participants;
    }

    private GameParticipant toParticipant(GameMember member) throws StorageException {
        validator.validateRole(member.getRole());
        Player player = gameStorage.getPlayer(member.getPlayerId());
        if (player == null || player.getUserId() == 0 || player.getDeviceId() == 0) {
            throw new IllegalArgumentException("Game member has invalid player assignment");
        }
        return new GameParticipant(player.getUserId(), player.getDeviceId(), member.getRole());
    }

    private void clearGameDevicePermissions(
            long userId, List<GameParticipant> participants, HttpServletRequest httpRequest) throws Exception {
        var participantUserIds = new HashSet<Long>();
        var participantDeviceIds = new HashSet<Long>();
        for (GameParticipant participant : participants) {
            participantUserIds.add(participant.userId());
            participantDeviceIds.add(participant.deviceId());
        }

        for (long participantUserId : participantUserIds) {
            for (long participantDeviceId : participantDeviceIds) {
                gamePermissionService.removePermission(
                        httpRequest, userId, participantUserId, Device.class, participantDeviceId);
            }
        }
    }

    private boolean canSeeDevice(String role, String targetRole) {
        if (GameMember.ROLE_GAME_MANAGEMENT.equals(role)) {
            return true;
        }
        if (GameMember.ROLE_GAME_MANAGEMENT.equals(targetRole)) {
            return true;
        }
        return role.equals(targetRole);
    }

}
