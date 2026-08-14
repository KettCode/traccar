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
        List<GameParticipant> participants = getParticipants(gameId);
        validateParticipants(participants);

        clearGameDevicePermissions(userId, participants, httpRequest);
        addGameDevicePermissions(userId, participants, httpRequest);
    }

    public void clearGameDevicePermissions(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        clearGameDevicePermissions(userId, getValidParticipants(gameId), httpRequest);
    }

    public void syncMemberDevicePermissions(
            long userId, long gameId, long memberId, HttpServletRequest httpRequest) throws Exception {
        List<GameParticipant> participants = getValidParticipants(gameId);
        GameParticipant member = findParticipant(participants, memberId);
        if (member == null) {
            return;
        }

        if (GameMember.STATUS_CAUGHT.equals(member.status())) {
            removeDeviceAccess(userId, participants, List.of(member), httpRequest);
            return;
        }

        removeDeviceAccess(userId, getDeniedReceivers(participants, member), List.of(member), httpRequest);
        grantDeviceAccess(userId, getAllowedReceivers(participants, member), List.of(member), httpRequest);
        removeDeviceAccess(userId, List.of(member), getDeniedMembers(participants, member), httpRequest);
        grantDeviceAccess(userId, List.of(member), getAllowedMembers(participants, member), httpRequest);
    }

    private void addGameDevicePermissions(
            long userId, List<GameParticipant> participants, HttpServletRequest httpRequest) throws Exception {
        for (GameParticipant participant : participants) {
            for (GameParticipant target : participants) {
                if (canSeeDevice(participant, target)) {
                    grantDeviceAccess(userId, participant, target, httpRequest);
                }
            }
        }
    }

    private void validateParticipants(List<GameParticipant> participants) {
        for (GameParticipant participant : participants) {
            validator.validateRole(participant.role());
            if (participant.userId() == 0 || participant.deviceId() == 0) {
                throw new IllegalArgumentException("Game member has invalid player assignment");
            }
        }
    }

    private List<GameParticipant> getParticipants(long gameId) throws StorageException {
        List<GameMember> members = gameStorage.getNonLeftGameMembers(gameId);
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        var participants = new ArrayList<GameParticipant>();
        for (GameMember member : members) {
            participants.add(getParticipant(member, playersById.get(member.getPlayerId())));
        }
        return participants;
    }

    private GameParticipant getParticipant(GameMember member, Player player) {
        long userId = player != null ? player.getUserId() : 0;
        long deviceId = player != null ? player.getDeviceId() : 0;
        return new GameParticipant(member.getId(), userId, deviceId, member.getRole(), member.getStatus());
    }

    private List<GameParticipant> getValidParticipants(long gameId) throws StorageException {
        return getParticipants(gameId).stream()
                .filter(this::isValidParticipant)
                .toList();
    }

    private boolean isValidParticipant(GameParticipant participant) {
        try {
            validator.validateRole(participant.role());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return participant.userId() != 0 && participant.deviceId() != 0;
    }

    private GameParticipant findParticipant(List<GameParticipant> participants, long memberId) {
        for (GameParticipant participant : participants) {
            if (participant.memberId() == memberId) {
                return participant;
            }
        }
        return null;
    }

    private List<GameParticipant> getAllowedReceivers(List<GameParticipant> participants, GameParticipant target) {
        return participants.stream()
                .filter(participant -> canSeeDevice(participant, target))
                .toList();
    }

    private List<GameParticipant> getDeniedReceivers(List<GameParticipant> participants, GameParticipant target) {
        return participants.stream()
                .filter(participant -> !canSeeDevice(participant, target))
                .toList();
    }

    private List<GameParticipant> getAllowedMembers(List<GameParticipant> participants, GameParticipant receiver) {
        return participants.stream()
                .filter(participant -> canSeeDevice(receiver, participant))
                .toList();
    }

    private List<GameParticipant> getDeniedMembers(List<GameParticipant> participants, GameParticipant receiver) {
        return participants.stream()
                .filter(participant -> !canSeeDevice(receiver, participant))
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
                    } else if (!isOwnDevice(receiver, visibleMember)) {
                        gamePermissionService.removePermission(
                                httpRequest, userId, receiver.userId(), Device.class, visibleMember.deviceId());
                    }
                }
            }
        }
    }

    private boolean isOwnDevice(GameParticipant receiver, GameParticipant visibleMember) {
        return receiver.userId() == visibleMember.userId()
                && receiver.deviceId() == visibleMember.deviceId();
    }

    private void clearGameDevicePermissions(
            long userId, List<GameParticipant> participants, HttpServletRequest httpRequest) throws Exception {
        for (GameParticipant participant : participants) {
            for (GameParticipant target : participants) {
                if (!isOwnDevice(participant, target)) {
                    gamePermissionService.removePermission(
                            httpRequest, userId, participant.userId(), Device.class, target.deviceId());
                }
            }
        }
    }

    private boolean canSeeDevice(GameParticipant receiver, GameParticipant target) {
        if (GameMember.STATUS_CAUGHT.equals(target.status())) {
            return receiver.memberId() == target.memberId();
        }
        if (GameMember.ROLE_GAME_MANAGEMENT.equals(receiver.role())) {
            return true;
        }
        if (GameMember.ROLE_GAME_MANAGEMENT.equals(target.role())) {
            return true;
        }
        return receiver.role().equals(target.role());
    }

}
