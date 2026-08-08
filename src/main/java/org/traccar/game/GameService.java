package org.traccar.game;

import jakarta.inject.Inject;
import org.traccar.model.Game;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class GameService {

    @Inject
    private Storage storage;

    @Inject
    private GamePermissionService gamePermissionService;

    public Game getGame(long gameId) throws StorageException {
        return storage.getObject(Game.class, new Request(
                new Columns.All(), new Condition.Equals("id", gameId)));
    }

    public Game getDraftGame(long gameId) throws StorageException {
        return getGameInStatus(gameId, Game.STATUS_DRAFT, "Game setup can only be changed for draft games");
    }

    public Game getRunningGame(long gameId) throws StorageException {
        return getGameInStatus(gameId, Game.STATUS_RUNNING, "Only running games can be changed");
    }

    public Game getAccessibleGame(long userId, long gameId) throws StorageException {
        gamePermissionService.checkPermission(userId, gameId);
        return getGame(gameId);
    }

    public Game getEditableDraftGame(long userId, long gameId) throws StorageException {
        gamePermissionService.checkEdit(userId, gameId);
        return getDraftGame(gameId);
    }

    public Game getEditableRunningGame(long userId, long gameId) throws StorageException {
        gamePermissionService.checkEdit(userId, gameId);
        return getRunningGame(gameId);
    }

    private Game getGameInStatus(long gameId, String status, String errorMessage) throws StorageException {
        Game game = getGame(gameId);
        if (game == null) {
            return null;
        }
        if (!status.equals(game.getStatus())) {
            throw new IllegalArgumentException(errorMessage);
        }
        return game;
    }

}
