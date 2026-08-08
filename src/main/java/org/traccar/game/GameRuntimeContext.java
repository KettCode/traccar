package org.traccar.game;

import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.Player;

public record GameRuntimeContext(long userId, Game game, GameMember member, Player player) {

    public boolean isGameManagement() {
        return GameMember.ROLE_GAME_MANAGEMENT.equals(member.getRole());
    }

    public boolean isHunter() {
        return GameMember.ROLE_HUNTER.equals(member.getRole());
    }

    public boolean isHunted() {
        return GameMember.ROLE_HUNTED.equals(member.getRole());
    }

    public boolean isActive() {
        return GameMember.STATUS_ACTIVE.equals(member.getStatus());
    }

    public boolean isCaught() {
        return GameMember.STATUS_CAUGHT.equals(member.getStatus());
    }

    public boolean isLeft() {
        return GameMember.STATUS_LEFT.equals(member.getStatus());
    }

}
