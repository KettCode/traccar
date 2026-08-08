package org.traccar.game;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;
import org.traccar.session.cache.CacheManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class GameLifecycleService {

    private record GameParticipant(long memberId, long userId, long deviceId, String role) {}

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GamePermissionService gamePermissionService;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    public Game activate(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }

        validator.validateSettings(game);
        List<GameParticipant> participants = getActiveParticipants(gameId);
        validateActivationParticipants(participants);
        syncGameDevicePermissions(userId, participants, httpRequest);

        Game update = new Game();
        update.setId(gameId);
        update.setStatus(Game.STATUS_RUNNING);
        update.setStartedAt(new Date());
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "startedAt", "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    public Game finish(long userId, long gameId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableRunningGame(userId, gameId);
        if (game == null) {
            return null;
        }

        clearGameDevicePermissions(userId, getActiveParticipants(gameId), httpRequest);

        Game update = new Game();
        update.setId(gameId);
        update.setStatus(Game.STATUS_FINISHED);
        update.setFinishedAt(new Date());
        update.setUpdatedAt(new Date());
        storage.updateObject(update, new Request(
                new Columns.Include("status", "finishedAt", "updatedAt"),
                new Condition.Equals("id", gameId)));

        cacheManager.invalidateObject(true, Game.class, gameId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, update);

        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    private List<GameParticipant> getActiveParticipants(long gameId) throws StorageException {
        var participants = new ArrayList<GameParticipant>();
        var members = storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
        for (GameMember member : members) {
            validator.validateRole(member.getRole());
            Player player = storage.getObject(Player.class, new Request(
                    new Columns.All(), new Condition.Equals("id", member.getPlayerId())));
            if (player == null || player.getUserId() == 0 || player.getDeviceId() == 0) {
                throw new IllegalArgumentException("Active game member has invalid player assignment");
            }
            participants.add(new GameParticipant(
                    member.getId(), player.getUserId(), player.getDeviceId(), member.getRole()));
        }
        return participants;
    }

    private void validateActivationParticipants(List<GameParticipant> participants) {
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

    private void syncGameDevicePermissions(
            long userId, List<GameParticipant> participants, HttpServletRequest httpRequest) throws Exception {
        clearGameDevicePermissions(userId, participants, httpRequest);

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
