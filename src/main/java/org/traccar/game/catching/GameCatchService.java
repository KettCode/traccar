package org.traccar.game.catching;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameDevicePermissionService;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.GamePushNotificationService;
import org.traccar.helper.LogAction;
import org.traccar.model.GameCatch;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GameSpeedhunt;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

public class GameCatchService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameNotificationService notificationService;

    @Inject
    private GamePushNotificationService pushNotificationService;

    @Inject
    private GameDevicePermissionService devicePermissionService;

    @Inject
    private GameStorage gameStorage;

    public GameCatch createCatch(
            long userId, long gameId, long caughtMemberId, String note, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireGameManagement(userId, gameId);
        if (context == null) {
            return null;
        }

        GameMember target = gameStorage.getGameMember(gameId, caughtMemberId);
        if (target == null) {
            throw new IllegalArgumentException("Caught member not found");
        }
        validateCatchTarget(target);
        if (gameStorage.getActiveGameCatchForMember(gameId, caughtMemberId) != null) {
            throw new IllegalArgumentException("Member already has an active catch");
        }

        Date caughtAt = new Date();
        GameCatch catchItem = new GameCatch();
        catchItem.setGameId(gameId);
        catchItem.setCaughtMemberId(caughtMemberId);
        catchItem.setReportedByUserId(userId);
        catchItem.setStatus(GameCatch.STATUS_ACTIVE);
        catchItem.setCaughtAt(caughtAt);
        catchItem.setNote(note);
        applyOptionalPosition(target, catchItem);
        catchItem.setId(storage.addObject(catchItem, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, catchItem);

        updateMemberCaught(userId, target, caughtAt, request);
        finishActiveSpeedhuntsForTarget(context, caughtMemberId, request);
        expireMemberRuntimeState(userId, gameId, caughtMemberId, request);
        devicePermissionService.applyCatchPermissions(userId, gameId, caughtMemberId, request);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_CATCH_CREATED);
        message.setCatchId(catchItem.getId());
        notificationService.notifyGameMembers(gameId, message);
        pushNotificationService.notifyCatchCreated(gameId);

        return catchItem;
    }

    public GameCatch revertCatch(long userId, long gameId, long catchId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireGameManagement(userId, gameId);
        if (context == null) {
            return null;
        }

        GameCatch catchItem = gameStorage.getGameCatch(gameId, catchId);
        if (catchItem == null) {
            return null;
        }
        if (!GameCatch.STATUS_ACTIVE.equals(catchItem.getStatus())) {
            throw new IllegalArgumentException("Only active catches can be reverted");
        }

        GameMember member = gameStorage.getGameMember(gameId, catchItem.getCaughtMemberId());
        if (member == null) {
            throw new IllegalArgumentException("Caught member not found");
        }

        Date revertedAt = new Date();
        GameCatch update = new GameCatch();
        update.setId(catchId);
        update.setStatus(GameCatch.STATUS_REVERTED);
        update.setRevertedAt(revertedAt);
        update.setRevertedByUserId(userId);
        storage.updateObject(update, new Request(
                new Columns.Include("status", "revertedAt", "revertedByUserId"),
                new Condition.Equals("id", catchId)));
        cacheManager.invalidateObject(true, GameCatch.class, catchId, ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        updateMemberActive(userId, member, request);
        devicePermissionService.applyCatchRevertedPermissions(userId, gameId, member.getId(), request);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_CATCH_REVERTED);
        message.setCatchId(catchId);
        notificationService.notifyGameMembers(gameId, message);
        pushNotificationService.notifyCatchReverted(gameId);
        return gameStorage.getGameCatch(gameId, catchId);
    }

    private void applyOptionalPosition(GameMember target, GameCatch catchItem) throws StorageException {
        Player player = gameStorage.getPlayer(target.getPlayerId());
        if (player == null || player.getDeviceId() == 0) {
            return;
        }

        Position position = gameStorage.getLatestPositionByDeviceId(player.getDeviceId());
        if (position == null) {
            return;
        }

        catchItem.setPositionId(position.getId());
        catchItem.setLatitude(position.getLatitude());
        catchItem.setLongitude(position.getLongitude());
    }

    private void updateMemberCaught(
            long userId, GameMember member, Date caughtAt, HttpServletRequest request) throws Exception {
        GameMember update = new GameMember();
        update.setId(member.getId());
        update.setStatus(GameMember.STATUS_CAUGHT);
        update.setCaughtAt(caughtAt);
        storage.updateObject(update, new Request(
                new Columns.Include("status", "caughtAt"),
                new Condition.Equals("id", member.getId())));
        cacheManager.invalidateObject(true, GameMember.class, member.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);
        member.setStatus(GameMember.STATUS_CAUGHT);
        member.setCaughtAt(caughtAt);
    }

    private void updateMemberActive(long userId, GameMember member, HttpServletRequest request) throws Exception {
        GameMember update = new GameMember();
        update.setId(member.getId());
        update.setStatus(GameMember.STATUS_ACTIVE);
        update.setCaughtAt(null);
        storage.updateObject(update, new Request(
                new Columns.Include("status", "caughtAt"),
                new Condition.Equals("id", member.getId())));
        cacheManager.invalidateObject(true, GameMember.class, member.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);
    }

    private void finishActiveSpeedhuntsForTarget(
            GameRuntimeContext context, long targetMemberId, HttpServletRequest request) throws Exception {
        for (GameSpeedhunt speedhunt : gameStorage.getActiveGameSpeedhuntsForTarget(context.game().getId(), targetMemberId)) {
            Date endedAt = new Date();
            GameSpeedhunt update = new GameSpeedhunt();
            update.setId(speedhunt.getId());
            update.setEndedAt(endedAt);
            storage.updateObject(update, new Request(
                    new Columns.Include("endedAt"),
                    new Condition.Equals("id", speedhunt.getId())));
            cacheManager.invalidateObject(true, GameSpeedhunt.class, speedhunt.getId(), ObjectOperation.UPDATE);
            actionLogger.edit(request, context.userId(), update);
        }
    }

    private void expireMemberRuntimeState(
            long userId, long gameId, long memberId, HttpServletRequest request) throws Exception {
        var effects = storage.getObjects(GamePendingEffect.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", gameId),
                                new Condition.Equals("memberId", memberId)),
                        new Condition.Equals("active", true)), new Order("id")));
        for (GamePendingEffect effect : effects) {
            GamePendingEffect update = new GamePendingEffect();
            update.setId(effect.getId());
            update.setActive(false);
            update.setConsumedAt(new Date());
            storage.updateObject(update, new Request(
                    new Columns.Include("active", "consumedAt"),
                    new Condition.Equals("id", effect.getId())));
            cacheManager.invalidateObject(true, GamePendingEffect.class, effect.getId(), ObjectOperation.UPDATE);
            actionLogger.edit(request, userId, update);
        }

        var jokers = storage.getObjects(GameJoker.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("memberId", memberId)), new Order("id")));
        for (GameJoker joker : jokers) {
            if (GameJoker.STATUS_UNLOCKED.equals(joker.getStatus())
                    || GameJoker.STATUS_ACTIVATED.equals(joker.getStatus())) {
                GameJoker update = new GameJoker();
                update.setId(joker.getId());
                update.setStatus(GameJoker.STATUS_EXPIRED);
                storage.updateObject(update, new Request(
                        new Columns.Include("status"),
                        new Condition.Equals("id", joker.getId())));
                cacheManager.invalidateObject(true, GameJoker.class, joker.getId(), ObjectOperation.UPDATE);
                actionLogger.edit(request, userId, update);
            }
        }
    }

    private void validateCatchTarget(GameMember member) {
        if (!GameMember.ROLE_HUNTED.equals(member.getRole())) {
            throw new IllegalArgumentException("Caught member must be a hunted player");
        }
        if (!GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            throw new IllegalArgumentException("Caught member must be active");
        }
    }

}
