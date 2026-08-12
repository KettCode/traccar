package org.traccar.game.notification;

import jakarta.inject.Inject;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.GameStorage;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.session.GameConnectionManager;
import org.traccar.model.GameMember;
import org.traccar.model.Player;
import org.traccar.storage.StorageException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameNotificationService {

    @Inject
    private GameConnectionManager gameConnectionManager;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

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

    public void notifyMember(long gameId, long memberId, GameNotificationMessage message) throws StorageException {
        GameMember member = gameStorage.getGameMember(gameId, memberId);
        if (member != null) {
            notifyMembers(List.of(member), message);
        }
    }

    private void notifyMembers(List<GameMember> members, GameNotificationMessage message) throws StorageException {
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        Set<Long> userIds = new HashSet<>();
        addMemberUserIds(userIds, members, playersById);
        notifyUsers(userIds, message);
    }

    private void notifyUsers(Set<Long> userIds, GameNotificationMessage message) {
        for (Long userId : userIds) {
            gameConnectionManager.updateGameNotification(userId, message);
        }
    }

    private void addMemberUserIds(Set<Long> userIds, List<GameMember> members, Map<Long, Player> playersById) {
        for (GameMember member : members) {
            addMemberUserId(userIds, member, playersById);
        }
    }

    private void addMemberUserId(Set<Long> userIds, GameMember member, Map<Long, Player> playersById) {
        if (!runtimePermissionService.canReceiveStateNotifications(member)) {
            return;
        }
        Player player = playersById.get(member.getPlayerId());
        if (player != null && player.getUserId() != 0) {
            userIds.add(player.getUserId());
        }
    }

}
