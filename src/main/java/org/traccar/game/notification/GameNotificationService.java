package org.traccar.game.notification;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.model.GameMember;
import org.traccar.model.Player;
import org.traccar.storage.StorageException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameNotificationService {

    @Inject
    private GameConnectionManager gameConnectionManager;

    @Inject
    private GameStorage gameStorage;

    public GameNotificationMessage createMessage(long gameId, String type, boolean stateRefresh) {
        GameNotificationMessage message = new GameNotificationMessage();
        message.setGameId(gameId);
        message.setType(type);
        message.setStateRefresh(stateRefresh);
        return message;
    }

    public GameNotificationMessage createStateChangedMessage(long gameId, String type) {
        return createMessage(gameId, type, true);
    }

    public void notifyGameMembers(long gameId, GameNotificationMessage message) throws StorageException {
        notifyMembers(gameStorage.getGameMembers(gameId), message);
    }

    public void notifyGameMembers(long gameId, String type, boolean stateRefresh) throws StorageException {
        notifyGameMembers(gameId, createMessage(gameId, type, stateRefresh));
    }

    public void notifyManagement(long gameId, GameNotificationMessage message) throws StorageException {
        notifyMembers(gameStorage.getGameMembersByRole(gameId, GameMember.ROLE_GAME_MANAGEMENT), message);
    }

    public void notifyRole(long gameId, String role, GameNotificationMessage message) throws StorageException {
        notifyMembers(gameStorage.getGameMembersByRole(gameId, role), message);
    }

    public void notifyMember(long gameId, long memberId, GameNotificationMessage message) throws StorageException {
        GameMember member = gameStorage.getGameMember(gameId, memberId);
        if (member != null) {
            notifyMembers(List.of(member), message);
        }
    }

    public void notifyManagementAndMember(
            long gameId, long memberId, GameNotificationMessage message) throws StorageException {
        Set<Long> userIds = new HashSet<>();
        addMemberUserIds(userIds, gameStorage.getGameMembersByRole(gameId, GameMember.ROLE_GAME_MANAGEMENT));
        GameMember member = gameStorage.getGameMember(gameId, memberId);
        if (member != null) {
            addMemberUserId(userIds, member);
        }
        notifyUsers(userIds, message);
    }

    private void notifyMembers(List<GameMember> members, GameNotificationMessage message) throws StorageException {
        Set<Long> userIds = new HashSet<>();
        addMemberUserIds(userIds, members);
        notifyUsers(userIds, message);
    }

    private void notifyUsers(Set<Long> userIds, GameNotificationMessage message) {
        for (Long userId : userIds) {
            gameConnectionManager.updateGameNotification(userId, message);
        }
    }

    private void addMemberUserIds(Set<Long> userIds, List<GameMember> members) throws StorageException {
        for (GameMember member : members) {
            addMemberUserId(userIds, member);
        }
    }

    private void addMemberUserId(Set<Long> userIds, GameMember member) throws StorageException {
        if (!canReceiveNotification(member)) {
            return;
        }
        Player player = gameStorage.getPlayer(member.getPlayerId());
        if (player != null && player.getUserId() != 0) {
            userIds.add(player.getUserId());
        }
    }

    private boolean canReceiveNotification(GameMember member) {
        return GameMember.STATUS_ACTIVE.equals(member.getStatus())
                || GameMember.STATUS_CAUGHT.equals(member.getStatus());
    }

}
