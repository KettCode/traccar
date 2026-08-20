package org.traccar.game;

import jakarta.inject.Inject;
import org.traccar.game.view.CurrentGameView;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.Player;
import org.traccar.storage.StorageException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameCurrentService {

    @Inject
    private GameStorage gameStorage;

    public CurrentGameView getCurrent(long userId) throws StorageException {
        Player player = gameStorage.getPlayerByUser(userId);
        if (player == null) {
            return null;
        }

        List<GameMember> members = gameStorage.getViewableGameMembersByPlayer(player.getId());
        Map<Long, GameMember> membersByGameId = indexMembersByGameId(members);
        List<Game> games = gameStorage.getGamesByMembers(members).stream()
                .filter(this::isSupportedStatus)
                .sorted(this::compareGames)
                .toList();

        for (Game game : games) {
            GameMember member = membersByGameId.get(game.getId());
            if (member != null) {
                return toView(game, member);
            }
        }
        return null;
    }

    private boolean isSupportedStatus(Game game) {
        return Game.STATUS_RUNNING.equals(game.getStatus())
                || Game.STATUS_DRAFT.equals(game.getStatus())
                || Game.STATUS_FINISHED.equals(game.getStatus());
    }

    private Map<Long, GameMember> indexMembersByGameId(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            GameMember previous = result.get(member.getGameId());
            if (previous == null || member.getId() > previous.getId()) {
                result.put(member.getGameId(), member);
            }
        }
        return result;
    }

    private int compareGames(Game first, Game second) {
        int statusCompare = Integer.compare(statusPriority(first), statusPriority(second));
        if (statusCompare != 0) {
            return statusCompare;
        }

        int dateCompare = Long.compare(getSortTime(second), getSortTime(first));
        if (dateCompare != 0) {
            return dateCompare;
        }

        return Long.compare(second.getId(), first.getId());
    }

    private int statusPriority(Game game) {
        return Game.STATUS_RUNNING.equals(game.getStatus()) ? 0 : 1;
    }

    private long getSortTime(Game game) {
        Date date = game.getUpdatedAt() != null ? game.getUpdatedAt() : game.getCreatedAt();
        return date != null ? date.getTime() : 0;
    }

    private CurrentGameView toView(Game game, GameMember member) {
        var view = new CurrentGameView();
        view.setId(game.getId());
        view.setName(game.getName());
        view.setStatus(game.getStatus());
        view.setMemberId(member.getId());
        view.setMemberDisplayName(member.getDisplayName());
        view.setMemberRole(member.getRole());
        view.setMemberStatus(member.getStatus());
        view.setReadonly(!Game.STATUS_RUNNING.equals(game.getStatus()));
        return view;
    }

}
