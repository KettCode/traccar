package org.traccar.game.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.traccar.api.security.LoginService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.SessionHelper;
import org.traccar.storage.StorageException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;

@Singleton
public class GameAsyncSocketServlet extends JettyWebSocketServlet {

    private final Config config;
    private final ObjectMapper objectMapper;
    private final LoginService loginService;
    private final GameConnectionManager gameConnectionManager;

    @Inject
    public GameAsyncSocketServlet(
            Config config, ObjectMapper objectMapper, LoginService loginService,
            GameConnectionManager gameConnectionManager) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.loginService = loginService;
        this.gameConnectionManager = gameConnectionManager;
    }

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(config.getLong(Keys.WEB_TIMEOUT)));
        factory.setCreator((req, resp) -> {
            Long userId = null;
            List<String> tokens = req.getParameterMap().get("token");
            if (tokens != null && !tokens.isEmpty()) {
                String token = tokens.iterator().next();
                try {
                    userId = loginService.login(token).getUser().getId();
                } catch (StorageException | GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (SessionHelper.isSessionOriginValid(req.getHttpServletRequest())) {
                userId = (Long) ((HttpSession) req.getSession()).getAttribute(SessionHelper.USER_ID_KEY);
            }
            if (userId != null) {
                return new GameAsyncSocket(objectMapper, gameConnectionManager, userId);
            }
            return null;
        });
    }

}
