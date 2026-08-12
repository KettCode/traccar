package org.traccar.game.member;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameDevicePermissionService;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.helper.LogAction;
import org.traccar.model.GameMember;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class GameMemberActionService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameDevicePermissionService devicePermissionService;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameNotificationService notificationService;

    public GameMember convertCaughtHuntedToHunter(
            long userId, long gameId, long memberId, HttpServletRequest request) throws Exception {
        if (runtimePermissionService.requireGameManagement(userId, gameId) == null) {
            return null;
        }

        GameMember member = gameStorage.getGameMember(gameId, memberId);
        if (member == null) {
            return null;
        }
        if (!GameMember.ROLE_HUNTED.equals(member.getRole())
                || !GameMember.STATUS_CAUGHT.equals(member.getStatus())) {
            throw new IllegalArgumentException("Only caught hunted members can be converted to hunters");
        }

        GameMember update = new GameMember();
        update.setId(memberId);
        update.setRole(GameMember.ROLE_HUNTER);
        update.setStatus(GameMember.STATUS_ACTIVE);
        update.setCanStartSpeedhunt(false);
        update.setCanRequestSpeedhuntPing(false);
        storage.updateObject(update, new Request(
                new Columns.Include("role", "status", "canStartSpeedhunt", "canRequestSpeedhuntPing"),
                new Condition.Equals("id", memberId)));
        cacheManager.invalidateObject(true, GameMember.class, memberId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        devicePermissionService.applyMemberConvertedToHunterPermissions(userId, gameId, memberId, request);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_MEMBER_CONVERTED_TO_HUNTER);
        message.setMemberId(memberId);
        notificationService.notifyGameMembers(gameId, message);

        return gameStorage.getGameMember(gameId, memberId);
    }

}
