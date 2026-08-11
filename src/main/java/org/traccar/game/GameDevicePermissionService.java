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
import java.util.Map;

public class GameDevicePermissionService {

    private record GameParticipant(long memberId, long userId, long deviceId, String role, String status) {}

    @Inject
    private GamePermissionService gamePermissionService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private GameStorage gameStorage;

    public void validateAndSyncGameDevicePermissions(
            long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        List<GameMember> members = gameStorage.getNonLeftGameMembers(gameId);
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        validateParticipantAssignments(members, playersById);

        List<GameParticipant> allParticipants = getParticipants(members, playersById);
        List<GameParticipant> activeParticipants = allParticipants.stream()
                .filter(participant -> GameMember.STATUS_ACTIVE.equals(participant.status()))
                .toList();
        validateRequiredActiveRoles(activeParticipants);

        clearGameDevicePermissions(userId, allParticipants, httpRequest);
        addGameDevicePermissions(userId, activeParticipants, httpRequest);
    }

    public void clearGameDevicePermissions(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        clearGameDevicePermissions(userId, getValidParticipants(gameId), httpRequest);
    }

    public void applyCatchPermissions(
            long userId, long gameId, long caughtMemberId, HttpServletRequest httpRequest) throws Exception {
        List<GameParticipant> participants = getValidParticipants(gameId);
        GameParticipant caught = findParticipant(participants, caughtMemberId);
        if (caught == null) {
            return;
        }

        removeDeviceAccess(userId, withoutMember(participants, caught.memberId()), List.of(caught), httpRequest);
    }

    public void applyCatchRevertedPermissions(
            long userId, long gameId, long revertedMemberId, HttpServletRequest httpRequest) throws Exception {
        List<GameParticipant> participants = getValidParticipants(gameId);
        GameParticipant reverted = findParticipant(participants, revertedMemberId);
        if (reverted == null) {
            return;
        }

        grantDeviceAccess(userId, getHuntedParticipants(participants), List.of(reverted), httpRequest);
    }

    public void applyMemberConvertedToHunterPermissions(
            long userId, long gameId, long convertedMemberId, HttpServletRequest httpRequest) throws Exception {
        List<GameParticipant> participants = getValidParticipants(gameId);
        GameParticipant converted = findParticipant(participants, convertedMemberId);
        if (converted == null) {
            return;
        }
        List<GameParticipant> hunters = getHunterParticipants(participants);
        List<GameParticipant> hunted = getHuntedParticipants(participants);

        removeDeviceAccess(userId, hunted, List.of(converted), httpRequest);
        grantDeviceAccess(userId, hunters, List.of(converted), httpRequest);
        removeDeviceAccess(userId, List.of(converted), hunted, httpRequest);
        grantDeviceAccess(userId, List.of(converted), hunters, httpRequest);
    }

    private void validateRequiredActiveRoles(List<GameParticipant> participants) {
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

    private void addGameDevicePermissions(
            long userId, List<GameParticipant> participants, HttpServletRequest httpRequest) throws Exception {
        for (GameParticipant participant : participants) {
            for (GameParticipant target : participants) {
                if (canSeeDevice(participant.role(), target.role())) {
                    grantDeviceAccess(userId, participant, target, httpRequest);
                }
            }
        }
    }

    private void validateParticipantAssignments(List<GameMember> members, Map<Long, Player> playersById) {
        for (GameMember member : members) {
            validator.validateRole(member.getRole());
            Player player = playersById.get(member.getPlayerId());
            if (player == null || player.getUserId() == 0 || player.getDeviceId() == 0) {
                throw new IllegalArgumentException("Game member has invalid player assignment");
            }
        }
    }

    private List<GameParticipant> getParticipants(List<GameMember> members, Map<Long, Player> playersById) {
        var participants = new ArrayList<GameParticipant>();
        for (GameMember member : members) {
            Player player = playersById.get(member.getPlayerId());
            participants.add(new GameParticipant(
                    member.getId(), player.getUserId(), player.getDeviceId(), member.getRole(), member.getStatus()));
        }
        return participants;
    }

    private List<GameParticipant> getValidParticipants(long gameId) throws StorageException {
        List<GameMember> members = gameStorage.getNonLeftGameMembers(gameId);
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        var participants = new ArrayList<GameParticipant>();
        for (GameMember member : members) {
            Player player = playersById.get(member.getPlayerId());
            if (isValidParticipant(member, player)) {
                participants.add(new GameParticipant(
                        member.getId(), player.getUserId(), player.getDeviceId(), member.getRole(), member.getStatus()));
            }
        }
        return participants;
    }

    private boolean isValidParticipant(GameMember member, Player player) {
        try {
            validator.validateRole(member.getRole());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return player != null && player.getUserId() != 0 && player.getDeviceId() != 0;
    }

    private GameParticipant findParticipant(List<GameParticipant> participants, long memberId) {
        for (GameParticipant participant : participants) {
            if (participant.memberId() == memberId) {
                return participant;
            }
        }
        return null;
    }

    private List<GameParticipant> withoutMember(List<GameParticipant> participants, long memberId) {
        return participants.stream()
                .filter(participant -> participant.memberId() != memberId)
                .toList();
    }

    private List<GameParticipant> getHunterParticipants(List<GameParticipant> participants) {
        return participants.stream()
                .filter(participant -> GameMember.ROLE_HUNTER.equals(participant.role()))
                .toList();
    }

    private List<GameParticipant> getHuntedParticipants(List<GameParticipant> participants) {
        return participants.stream()
                .filter(participant -> GameMember.ROLE_HUNTED.equals(participant.role()))
                .toList();
    }

    private void grantDeviceAccess(
            long userId, List<GameParticipant> receivers, List<GameParticipant> visibleMembers,
            HttpServletRequest httpRequest) throws Exception {
        updateDeviceAccess(userId, receivers, visibleMembers, httpRequest, true);
    }

    private void grantDeviceAccess(
            long userId, GameParticipant receiver, GameParticipant visibleMember,
            HttpServletRequest httpRequest) throws Exception {
        gamePermissionService.addPermission(httpRequest, userId, receiver.userId(), Device.class, visibleMember.deviceId());
    }

    private void removeDeviceAccess(
            long userId, List<GameParticipant> receivers, List<GameParticipant> visibleMembers,
            HttpServletRequest httpRequest) throws Exception {
        updateDeviceAccess(userId, receivers, visibleMembers, httpRequest, false);
    }

    private void updateDeviceAccess(
            long userId, List<GameParticipant> receivers, List<GameParticipant> visibleMembers,
            HttpServletRequest httpRequest, boolean grant) throws Exception {
        var updated = new HashSet<String>();
        for (GameParticipant receiver : receivers) {
            for (GameParticipant visibleMember : visibleMembers) {
                String key = receiver.userId() + ":" + visibleMember.deviceId();
                if (updated.add(key)) {
                    if (grant) {
                        gamePermissionService.addPermission(
                                httpRequest, userId, receiver.userId(), Device.class, visibleMember.deviceId());
                    } else {
                        gamePermissionService.removePermission(
                                httpRequest, userId, receiver.userId(), Device.class, visibleMember.deviceId());
                    }
                }
            }
        }
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
