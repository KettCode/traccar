package org.traccar.game.notification;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.game.GameStorage;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.Typed;
import org.traccar.model.User;
import org.traccar.notificators.Notificator;
import org.traccar.notification.NotificationMessage;
import org.traccar.notification.NotificatorManager;
import org.traccar.storage.StorageException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GamePushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GamePushNotificationService.class);
    private static final String NOTIFICATOR_TRACCAR = "traccar";
    private static final String SUBJECT = "Stadtjagd";

    @Inject
    private NotificatorManager notificatorManager;

    @Inject
    private GameStorage gameStorage;

    public void notifySpeedhuntStarted(long gameId) throws StorageException {
        notifyMembers(gameStorage.getNonLeftGameMembers(gameId), "Ein neuer Speedhunt wurde gestartet.", true);
    }

    public void notifySpeedhuntPingCreated(long gameId) throws StorageException {
        notifyMembers(gameStorage.getNonLeftGameMembers(gameId), "Ein Speedhunt-Ping wurde angefordert.", true);
    }

    public void notifyCatchCreated(long gameId) throws StorageException {
        notifyMembers(gameStorage.getNonLeftGameMembers(gameId), "Ein Spieler wurde gefangen.", true);
    }

    public void notifyCatchReverted(long gameId) throws StorageException {
        notifyMembers(gameStorage.getNonLeftGameMembers(gameId), "Ein Catch wurde zurueckgenommen.", true);
    }

    public void notifyRegularPingsCreated(long gameId) throws StorageException {
        notifyMembers(gameStorage.getNonLeftGameMembers(gameId), "Standorte wurden aktualisiert.", true);
    }

    public void notifyJokerUnlocked(long gameId, GameJoker joker) throws StorageException {
        GameMember member = gameStorage.getGameMember(gameId, joker.getMemberId());
        if (member != null && GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            notifyMembers(List.of(member), "Ein Joker wurde fuer dich freigeschaltet.", false);
        }
    }

    public void notifyJokerActivated(long gameId, GameJoker joker) throws StorageException {
        GameMember member = gameStorage.getGameMember(gameId, joker.getMemberId());
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

    public void notifyLocationsMissingForManagement(long gameId, List<GameMember> targets) throws StorageException {
        if (targets.isEmpty()) {
            return;
        }
        notifyMembers(
                gameStorage.getActiveManagementMembers(gameId),
                "Keine aktuellen Standorte fuer " + formatMemberNames(targets) + ".", true);
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
        if (members.isEmpty() || !isTraccarEnabled()) {
            return;
        }

        Notificator notificator = getTraccarNotificator();
        if (notificator == null) {
            return;
        }

        NotificationMessage message = new NotificationMessage(SUBJECT, body, body, priority);
        Map<Long, User> usersByMemberId = gameStorage.getUsersByMembers(members);
        Set<Long> notifiedUserIds = new HashSet<>();
        for (GameMember member : members) {
            User user = usersByMemberId.get(member.getId());
            if (user == null || user.getDisabled() || !notifiedUserIds.add(user.getId())) {
                continue;
            }
            notificator.sendAsync(user, message, null, null).exceptionally(throwable -> {
                LOGGER.warn("Game push notification failed for user {}", user.getId(), throwable);
                return null;
            });
        }
    }

    private boolean isTraccarEnabled() {
        for (Typed type : notificatorManager.getAllNotificatorTypes()) {
            if (NOTIFICATOR_TRACCAR.equals(type.type())) {
                return true;
            }
        }
        return false;
    }

    private Notificator getTraccarNotificator() {
        try {
            return notificatorManager.getNotificator(NOTIFICATOR_TRACCAR);
        } catch (RuntimeException e) {
            LOGGER.debug("Traccar notificator unavailable", e);
            return null;
        }
    }

}
