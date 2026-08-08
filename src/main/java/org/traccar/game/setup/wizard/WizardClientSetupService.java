package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.WebHelper;
import org.traccar.protocol.OsmAndProtocol;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WizardClientSetupService {

    private static final String CLIENT_SCHEME = "org.traccar.client";
    private static final String PROTOCOL = BaseProtocol.nameFromClass(OsmAndProtocol.class);

    private static final String ACCURACY = "highest";
    private static final int DISTANCE = 10;
    private static final int INTERVAL = 30;
    private static final int ANGLE = 30;
    private static final int HEARTBEAT = 60;
    private static final boolean BUFFER = true;
    private static final boolean WAKELOCK = true;
    private static final boolean STOP_DETECTION = false;
    private static final boolean PREFER_PLATFORM_PROVIDERS = false;

    @Inject
    private Config config;

    public String buildSetupLink(String deviceIdentifier) {
        if (deviceIdentifier == null || deviceIdentifier.isBlank()) {
            return null;
        }

        return CLIENT_SCHEME + "://setup?"
                + parameter("url", getClientServerUrl())
                + "&" + parameter("id", deviceIdentifier)
                + "&" + parameter("accuracy", ACCURACY)
                + "&" + parameter("distance", DISTANCE)
                + "&" + parameter("interval", INTERVAL)
                + "&" + parameter("angle", ANGLE)
                + "&" + parameter("heartbeat", HEARTBEAT)
                + "&" + parameter("buffer", BUFFER)
                + "&" + parameter("wakelock", WAKELOCK)
                + "&" + parameter("stop_detection", STOP_DETECTION)
                + "&" + parameter("prefer_platform_providers", PREFER_PLATFORM_PROVIDERS);
    }

    private String getClientServerUrl() {
        URI webUri = URI.create(WebHelper.retrieveWebUrl(config));
        String scheme = config.getBoolean(Keys.PROTOCOL_SSL.withPrefix(PROTOCOL)) ? "https" : "http";
        int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(PROTOCOL));
        return URI.create(scheme + "://" + webUri.getHost() + ":" + port + "/").toString();
    }

    private String parameter(String key, Object value) {
        return key + "=" + URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

}
