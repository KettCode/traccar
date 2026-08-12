package org.traccar.game.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.game.map.message.GameMapUpdateMessage;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.session.GameConnectionManager;

import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GameAsyncSocket implements Session.Listener.AutoDemanding, GameConnectionManager.UpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameAsyncSocket.class);

    private static final String KEY_GAME_NOTIFICATIONS = "gameNotifications";
    private static final String KEY_GAME_MAP_UPDATES = "gameMapUpdates";

    private final ObjectMapper objectMapper;
    private final GameConnectionManager gameConnectionManager;
    private final long userId;

    private Session session;

    public GameAsyncSocket(ObjectMapper objectMapper, GameConnectionManager gameConnectionManager, long userId) {
        this.objectMapper = objectMapper;
        this.gameConnectionManager = gameConnectionManager;
        this.userId = userId;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        gameConnectionManager.addListener(userId, this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        gameConnectionManager.removeListener(userId, this);
        session = null;
        callback.succeed();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (!(cause instanceof ClosedChannelException)) {
            LOGGER.warn("Game WebSocket error", cause);
        }
    }

    @Override
    public void onKeepalive() {
        sendData(Map.of());
    }

    @Override
    public void onUpdateGameNotification(GameNotificationMessage notification) {
        sendData(Map.of(KEY_GAME_NOTIFICATIONS, List.of(notification)));
    }

    @Override
    public void onUpdateGameMap(GameMapUpdateMessage update) {
        sendData(Map.of(KEY_GAME_MAP_UPDATES, List.of(update)));
    }

    private void sendData(Map<String, Collection<?>> data) {
        if (session != null && session.isOpen()) {
            try {
                session.sendText(objectMapper.writeValueAsString(data), Callback.NOOP);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Game WebSocket JSON formatting error", e);
            }
        }
    }

}
