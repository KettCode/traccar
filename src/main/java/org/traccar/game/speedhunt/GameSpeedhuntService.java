package org.traccar.game.speedhunt;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.GameMapUpdateService;
import org.traccar.game.notification.GameNotificationMessage;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.GamePushNotificationService;
import org.traccar.game.ping.GamePingService;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.GameSpeedhunt;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

public class GameSpeedhuntService {

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
    private GameMapUpdateService mapUpdateService;

    @Inject
    private GamePushNotificationService pushNotificationService;

    @Inject
    private GamePingService pingService;

    public GameSpeedhunt startSpeedhunt(
            long userId, long gameId, long targetMemberId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanStartSpeedhunt(userId, gameId);
        if (context == null) {
            return null;
        }

        Game game = context.game();
        if (game.getSpeedhuntLimit() <= 0) {
            throw new IllegalArgumentException("Speedhunts are disabled");
        }

        List<GameSpeedhunt> speedhunts = getSpeedhunts(gameId);
        if (speedhunts.size() >= game.getSpeedhuntLimit()) {
            throw new IllegalArgumentException("No speedhunts remaining");
        }
        if (getActiveSpeedhunt(speedhunts) != null) {
            throw new IllegalArgumentException("A speedhunt is already active");
        }

        GameMember target = getTargetMember(gameId, targetMemberId);
        if (target == null) {
            throw new IllegalArgumentException("Speedhunt target not found");
        }
        validateTarget(target);

        GameSpeedhunt previous = speedhunts.isEmpty() ? null : speedhunts.get(speedhunts.size() - 1);
        if (previous != null
                && !game.getAllowConsecutiveSpeedhuntsSameTarget()
                && previous.getTargetMemberId() == targetMemberId) {
            throw new IllegalArgumentException("Consecutive speedhunts on the same target are not allowed");
        }

        GameSpeedhunt speedhunt = new GameSpeedhunt();
        speedhunt.setGameId(gameId);
        speedhunt.setSequenceNumber(speedhunts.size() + 1);
        speedhunt.setTargetMemberId(targetMemberId);
        speedhunt.setCreatedByUserId(userId);
        speedhunt.setMaxPings(game.getSpeedhuntPingLimit());
        speedhunt.setStartedAt(new Date());
        speedhunt.setId(storage.addObject(speedhunt, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, userId, speedhunt);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                gameId, GameNotificationMessage.TYPE_SPEEDHUNT_STARTED);
        message.setSpeedhuntId(speedhunt.getId());
        notificationService.notifyGameMembers(gameId, message);
        pushNotificationService.notifySpeedhuntStarted(gameId);

        createSpeedhuntPing(context, speedhunt, request, false);

        return speedhunt;
    }

    public GamePing requestSpeedhuntPing(
            long userId, long gameId, long speedhuntId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireCanRequestSpeedhuntPing(userId, gameId);
        if (context == null) {
            return null;
        }

        GameSpeedhunt speedhunt = getSpeedhunt(gameId, speedhuntId);
        if (speedhunt == null) {
            return null;
        }
        return createSpeedhuntPing(context, speedhunt, request, true);
    }

    public GameSpeedhunt finishSpeedhunt(
            long userId, long gameId, long speedhuntId, HttpServletRequest request) throws Exception {
        GameRuntimeContext context = runtimePermissionService.requireGameManagement(userId, gameId);
        if (context == null) {
            return null;
        }

        GameSpeedhunt speedhunt = getSpeedhunt(gameId, speedhuntId);
        if (speedhunt == null) {
            return null;
        }
        finishSpeedhunt(context, speedhunt, request);
        return speedhunt;
    }

    private GamePing createSpeedhuntPing(
            GameRuntimeContext context, GameSpeedhunt speedhunt, HttpServletRequest request,
            boolean notifyPush) throws Exception {
        if (speedhunt.getEndedAt() != null) {
            throw new IllegalArgumentException("Speedhunt is not active");
        }

        GameMember target = getTargetMember(context.game().getId(), speedhunt.getTargetMemberId());
        if (target == null) {
            throw new IllegalArgumentException("Speedhunt target not found");
        }
        validateTarget(target);

        List<GamePing> pings = getSpeedhuntPings(context.game().getId(), speedhunt.getId());
        if (pings.size() >= speedhunt.getMaxPings()) {
            throw new IllegalArgumentException("Speedhunt ping limit reached");
        }

        GamePendingEffect effect = pingService.getNextPendingEffect(context.game().getId(), target.getId());
        GamePing ping = new GamePing();
        ping.setGameId(context.game().getId());
        ping.setTargetMemberId(target.getId());
        ping.setSpeedhuntId(speedhunt.getId());
        ping.setSequenceNumber(pings.size() + 1);
        ping.setCreatedAt(new Date());

        if (effect != null) {
            pingService.applyPendingEffect(ping, effect, GamePing.SOURCE_SPEEDHUNT);
        } else {
            pingService.applyRealPosition(context.game(), target, ping, GamePing.SOURCE_SPEEDHUNT);
        }

        ping.setId(storage.addObject(ping, new Request(new Columns.Exclude("id"))));
        actionLogger.create(request, context.userId(), ping);

        if (effect != null) {
            pingService.consumeEffect(context.userId(), effect, ping, request);
        }

        mapUpdateService.notifySpeedhuntPingCreated(ping);
        if (notifyPush) {
            pushNotificationService.notifySpeedhuntPingCreated(context.game().getId());
        }

        if (ping.getSequenceNumber() >= speedhunt.getMaxPings()) {
            finishSpeedhunt(context, speedhunt, request);
        }

        return ping;
    }

    private void finishSpeedhunt(
            GameRuntimeContext context, GameSpeedhunt speedhunt, HttpServletRequest request) throws Exception {
        if (speedhunt.getEndedAt() != null) {
            return;
        }

        Date endedAt = new Date();
        GameSpeedhunt update = new GameSpeedhunt();
        update.setId(speedhunt.getId());
        update.setEndedAt(endedAt);
        storage.updateObject(update, new Request(
                new Columns.Include("endedAt"),
                new Condition.Equals("id", speedhunt.getId())));
        cacheManager.invalidateObject(true, GameSpeedhunt.class, speedhunt.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, context.userId(), update);
        speedhunt.setEndedAt(endedAt);

        GameNotificationMessage message = notificationService.createStateChangedMessage(
                context.game().getId(), GameNotificationMessage.TYPE_SPEEDHUNT_FINISHED);
        message.setSpeedhuntId(speedhunt.getId());
        notificationService.notifyGameMembers(context.game().getId(), message);
    }

    private void validateTarget(GameMember target) {
        if (!GameMember.ROLE_HUNTED.equals(target.getRole())) {
            throw new IllegalArgumentException("Speedhunt target must be a hunted player");
        }
        if (!GameMember.STATUS_ACTIVE.equals(target.getStatus())) {
            throw new IllegalArgumentException("Speedhunt target must be active");
        }
    }

    private GameMember getTargetMember(long gameId, long targetMemberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", targetMemberId),
                        new Condition.Equals("gameId", gameId))));
    }

    private List<GameSpeedhunt> getSpeedhunts(long gameId) throws StorageException {
        return storage.getObjects(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("sequenceNumber")));
    }

    private GameSpeedhunt getSpeedhunt(long gameId, long speedhuntId) throws StorageException {
        return storage.getObject(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", speedhuntId),
                        new Condition.Equals("gameId", gameId))));
    }

    private GameSpeedhunt getActiveSpeedhunt(List<GameSpeedhunt> speedhunts) {
        for (GameSpeedhunt speedhunt : speedhunts) {
            if (speedhunt.getEndedAt() == null) {
                return speedhunt;
            }
        }
        return null;
    }

    private List<GamePing> getSpeedhuntPings(long gameId, long speedhuntId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("speedhuntId", speedhuntId)), new Order("sequenceNumber")));
    }

}
