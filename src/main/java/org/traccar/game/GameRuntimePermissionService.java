package org.traccar.game;

import jakarta.inject.Inject;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GameReveal;
import org.traccar.model.GameSpeedhunt;
import org.traccar.model.Player;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class GameRuntimePermissionService {

    @Inject
    private Storage storage;

    @Inject
    private GameService gameService;

    @Inject
    private GameStorage gameStorage;

    public GameRuntimeContext getContext(long userId, long gameId) throws StorageException {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return null;
        }
        if (!Game.STATUS_RUNNING.equals(game.getStatus())) {
            throw new IllegalArgumentException("Runtime permissions require a running game");
        }

        Player player = gameStorage.getPlayerByUser(userId);
        if (player == null) {
            throw new SecurityException("Game member access denied");
        }

        GameMember member = gameStorage.getGameMemberByPlayer(gameId, player.getId());
        if (member == null) {
            throw new SecurityException("Game member access denied");
        }

        return new GameRuntimeContext(userId, game, member, player);
    }

    public GameRuntimeContext requireRunningMember(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = getContext(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!context.isActive() && !context.isCaught()) {
            throw new SecurityException("Game member access denied");
        }
        return context;
    }

    public GameRuntimeContext requireActiveMember(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = requireRunningMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!context.isActive()) {
            throw new SecurityException("Active game member required");
        }
        return context;
    }

    public GameRuntimeContext requireGameManagement(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = requireActiveMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!context.isGameManagement()) {
            throw new SecurityException("Game management access required");
        }
        return context;
    }

    public GameRuntimeContext requireCanStartSpeedhunt(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = requireActiveMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!canStartSpeedhunt(context)) {
            throw new SecurityException("Speedhunt start access denied");
        }
        return context;
    }

    public GameRuntimeContext requireCanRequestSpeedhuntPing(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = requireActiveMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!canRequestSpeedhuntPing(context)) {
            throw new SecurityException("Speedhunt ping access denied");
        }
        return context;
    }

    public GameRuntimeContext requireCanUseJoker(long userId, long gameId, long jokerId) throws StorageException {
        GameRuntimeContext context = requireActiveMember(userId, gameId);
        if (context == null) {
            return null;
        }

        GameJoker joker = gameStorage.getGameJoker(gameId, jokerId);
        if (joker == null) {
            throw new SecurityException("Joker access denied");
        }
        if (!canUseJoker(context, joker)) {
            throw new SecurityException("Joker access denied");
        }
        return context;
    }

    public boolean canStartSpeedhunt(GameRuntimeContext context) {
        return context.isActive() && (context.isGameManagement()
                || context.isHunter() && context.member().getCanStartSpeedhunt());
    }

    public boolean canRequestSpeedhuntPing(GameRuntimeContext context) {
        return context.isActive() && (context.isGameManagement()
                || context.isHunter() && context.member().getCanRequestSpeedhuntPing());
    }

    public boolean canUseJoker(GameRuntimeContext context, GameJoker joker) {
        if (!context.isActive()) {
            return false;
        }
        if (context.isGameManagement()) {
            return true;
        }
        return context.isHunted() && joker.getMemberId() == context.member().getId();
    }

    public boolean canViewJoker(GameRuntimeContext context, GameJoker joker) {
        if (context.isGameManagement()) {
            return true;
        }
        return context.isHunted() && joker.getMemberId() == context.member().getId();
    }

    public boolean canViewReveal(GameRuntimeContext context, GameReveal reveal) {
        return context.isGameManagement() || reveal.getMemberId() == context.member().getId();
    }

    public boolean canViewGeofence(GameRuntimeContext context, GameGeofence geofence) {
        if (!geofence.getActive()) {
            return false;
        }
        return context.isGameManagement()
                || geofence.getRole() == null
                || geofence.getRole().equals(context.member().getRole());
    }

    public boolean canViewSpeedhuntTarget(GameRuntimeContext context, GameSpeedhunt speedhunt)
            throws StorageException {
        if (context.isGameManagement() || context.isHunter()) {
            return true;
        }
        if (!context.isHunted()) {
            return false;
        }

        var reveals = storage.getObjects(GameReveal.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", context.game().getId()),
                                new Condition.Equals("memberId", context.member().getId())),
                        new Condition.And(
                                new Condition.Equals("type", GameReveal.TYPE_SPEEDHUNT_TARGET),
                                new Condition.Equals("speedhuntId", speedhunt.getId())))));
        for (GameReveal reveal : reveals) {
            if (reveal.getInvalidatedAt() == null) {
                return true;
            }
        }
        return false;
    }

}
