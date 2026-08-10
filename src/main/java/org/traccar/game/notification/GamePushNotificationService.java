package org.traccar.game.notification;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.Player;
import org.traccar.model.Typed;
import org.traccar.model.User;
import org.traccar.notificators.Notificator;
import org.traccar.notification.NotificationMessage;
import org.traccar.notification.NotificatorManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GamePushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GamePushNotificationService.class);
    private static final String NOTIFICATOR_FIREBASE = "firebase";
    private static final String SUBJECT = "Stadtjagd";

    @Inject
    private Storage storage;

    @Inject
    private NotificatorManager notificatorManager;

    public void notifySpeedhuntStarted(long gameId) throws StorageException {
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> GameMember.STATUS_ACTIVE.equals(member.getStatus()))
                .filter(member -> GameMember.ROLE_HUNTER.equals(member.getRole())
                        || GameMember.ROLE_GAME_MANAGEMENT.equals(member.getRole()))
                .toList(), "Ein neuer Speedhunt wurde gestartet.", true);
    }

    public void notifySpeedhuntPingCreated(long gameId) throws StorageException {
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> GameMember.STATUS_ACTIVE.equals(member.getStatus()))
                .filter(member -> GameMember.ROLE_HUNTER.equals(member.getRole())
                        || GameMember.ROLE_GAME_MANAGEMENT.equals(member.getRole()))
                .toList(), "Ein Speedhunt-Ping wurde angefordert.", true);
    }

    public void notifyCatchCreated(long gameId) throws StorageException {
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> !GameMember.STATUS_LEFT.equals(member.getStatus()))
                .toList(), "Ein Spieler wurde gefangen.", true);
    }

    public void notifyCatchReverted(long gameId) throws StorageException {
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> !GameMember.STATUS_LEFT.equals(member.getStatus()))
                .toList(), "Ein Catch wurde zurueckgenommen.", true);
    }

    public void notifyRegularPingsCreated(long gameId) throws StorageException {
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> !GameMember.STATUS_LEFT.equals(member.getStatus()))
                .toList(), "Standorte wurden aktualisiert.", true);
    }

    public void notifyJokerUnlocked(long gameId, GameJoker joker) throws StorageException {
        GameMember member = getMember(gameId, joker.getMemberId());
        if (member != null && GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            notifyMembers(List.of(member), "Ein Joker wurde fuer dich freigeschaltet.", false);
        }
    }

    public void notifyJokerActivated(long gameId, GameJoker joker) throws StorageException {
        GameMember member = getMember(gameId, joker.getMemberId());
        if (member != null && GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            notifyMembers(List.of(member), "Ein Joker wurde fuer dich aktiviert.", false);
        }
    }

    public void notifyOwnLocationMissing(List<GameMember> targets) throws StorageException {
        if (targets.isEmpty()) {
            return;
        }
        notifyMembers(targets, "Dein Standort ist nicht aktuell. Bitte pruefe deine Standortfreigabe.", true);
    }

    public void notifyRegularPingLocationsMissing(long gameId, List<GameMember> targets) throws StorageException {
        if (targets.isEmpty()) {
            return;
        }
        notifyMembers(getMembers(gameId).stream()
                .filter(member -> GameMember.STATUS_ACTIVE.equals(member.getStatus()))
                .filter(member -> GameMember.ROLE_GAME_MANAGEMENT.equals(member.getRole()))
                .toList(), "Keine aktuellen Standorte fuer Regular-Pings von " + formatMemberNames(targets) + ".", true);
    }

    private String formatMemberNames(List<GameMember> members) {
        List<String> names = members.stream()
                .map(member -> member.getDisplayName() != null ? member.getDisplayName() : "Ein Spieler")
                .toList();
        if (names.size() == 1) {
            return names.get(0);
        } else if (names.size() == 2) {
            return names.get(0) + " und " + names.get(1);
        } else if (names.size() == 3) {
            return names.get(0) + ", " + names.get(1) + " und " + names.get(2);
        }
        return names.get(0) + ", " + names.get(1) + " und " + (names.size() - 2) + " weiteren";
    }

    private void notifyMembers(List<GameMember> members, String body, boolean priority) throws StorageException {
        if (members.isEmpty() || !isFirebaseEnabled()) {
            return;
        }

        Notificator notificator = getFirebaseNotificator();
        if (notificator == null) {
            return;
        }

        NotificationMessage message = new NotificationMessage(SUBJECT, null, body, priority);
        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : members) {
            User user = getUser(member);
            if (user == null || user.getDisabled() || !notifiedUserIds.add(user.getId())) {
                continue;
            }
            notificator.sendAsync(user, message, null, null).exceptionally(throwable -> {
                LOGGER.warn("Game push notification failed for user {}", user.getId(), throwable);
                return null;
            });
        }
    }

    private boolean isFirebaseEnabled() {
        for (Typed type : notificatorManager.getAllNotificatorTypes()) {
            if (NOTIFICATOR_FIREBASE.equals(type.type())) {
                return true;
            }
        }
        return false;
    }

    private Notificator getFirebaseNotificator() {
        try {
            return notificatorManager.getNotificator(NOTIFICATOR_FIREBASE);
        } catch (RuntimeException e) {
            LOGGER.debug("Firebase notificator unavailable", e);
            return null;
        }
    }

    private User getUser(GameMember member) throws StorageException {
        Player player = storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", member.getPlayerId())));
        if (player == null || player.getUserId() == 0) {
            return null;
        }
        return storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", player.getUserId())));
    }

    private GameMember getMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", memberId),
                        new Condition.Equals("gameId", gameId))));
    }

    private List<GameMember> getMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

}
