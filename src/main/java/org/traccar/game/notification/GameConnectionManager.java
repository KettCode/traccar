package org.traccar.game.notification;

import jakarta.inject.Singleton;
import org.traccar.game.map.GameMapUpdateMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class GameConnectionManager {

    private final Map<Long, Set<UpdateListener>> listeners = new HashMap<>();

    public synchronized void updateGameNotification(long userId, GameNotificationMessage notification) {
        Set<UpdateListener> userListeners = listeners.get(userId);
        if (userListeners != null) {
            for (UpdateListener listener : userListeners) {
                listener.onUpdateGameNotification(notification);
            }
        }
    }

    public synchronized void updateGameMap(long userId, GameMapUpdateMessage update) {
        Set<UpdateListener> userListeners = listeners.get(userId);
        if (userListeners != null) {
            for (UpdateListener listener : userListeners) {
                listener.onUpdateGameMap(update);
            }
        }
    }

    public synchronized void addListener(long userId, UpdateListener listener) {
        listeners.computeIfAbsent(userId, key -> new HashSet<>()).add(listener);
    }

    public synchronized void removeListener(long userId, UpdateListener listener) {
        Set<UpdateListener> userListeners = listeners.get(userId);
        if (userListeners != null) {
            userListeners.remove(listener);
            if (userListeners.isEmpty()) {
                listeners.remove(userId);
            }
        }
    }

    public synchronized void sendKeepalive() {
        for (Set<UpdateListener> userListeners : listeners.values()) {
            for (UpdateListener listener : userListeners) {
                listener.onKeepalive();
            }
        }
    }

    public interface UpdateListener {
        void onKeepalive();
        void onUpdateGameNotification(GameNotificationMessage notification);
        void onUpdateGameMap(GameMapUpdateMessage update);
    }

}
