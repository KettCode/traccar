package org.traccar.game.ping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.game.GameStorage;
import org.traccar.helper.LogAction;
import org.traccar.model.Game;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GamePingService {

    @Inject
    private Storage storage;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private GameStorage gameStorage;

    public GamePendingEffect getNextPendingEffect(long gameId, long memberId) throws StorageException {
        return getNextPendingEffects(gameId, Set.of(memberId)).get(memberId);
    }

    public Map<Long, GamePendingEffect> getNextPendingEffects(long gameId, Set<Long> memberIds) throws StorageException {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        List<GamePendingEffect> effects = storage.getObjects(GamePendingEffect.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("active", true)), new Order("id")));

        var result = new HashMap<Long, GamePendingEffect>();
        for (GamePendingEffect effect : effects) {
            if (!memberIds.contains(effect.getMemberId())) {
                continue;
            }
            if (GamePendingEffect.EFFECT_SKIP_NEXT_PING.equals(effect.getEffect())) {
                result.put(effect.getMemberId(), effect);
            } else if (GamePendingEffect.EFFECT_FAKE_NEXT_PING.equals(effect.getEffect())
                    && !result.containsKey(effect.getMemberId())) {
                result.put(effect.getMemberId(), effect);
            }
        }
        return result;
    }

    public void applyPendingEffect(GamePing ping, GamePendingEffect effect, String clientSource) {
        if (GamePendingEffect.EFFECT_SKIP_NEXT_PING.equals(effect.getEffect())) {
            ping.setSource(clientSource);
            ping.setSkipped(true);
            ping.setConsumedJokerId(effect.getJokerId());
        } else if (GamePendingEffect.EFFECT_FAKE_NEXT_PING.equals(effect.getEffect())) {
            applyFakePing(ping, effect);
        }
    }

    public void applyRealPosition(Game game, GameMember target, GamePing ping, String source) throws StorageException {
        Player player = gameStorage.getPlayer(target.getPlayerId());
        if (player == null || player.getDeviceId() == 0) {
            throw new IllegalArgumentException("Ping target has no valid device");
        }

        Position position = gameStorage.getLatestPositionByDeviceId(player.getDeviceId());
        if (position == null) {
            throw new IllegalArgumentException("No position found for ping target");
        }
        applyPosition(game, ping, position, source);
    }

    public boolean isPositionValid(Game game, Position position) {
        return position != null && !isPositionTooOld(game, position);
    }

    public void consumeEffect(
            long userId, GamePendingEffect effect, GamePing ping, HttpServletRequest request) throws Exception {
        GamePendingEffect update = new GamePendingEffect();
        update.setId(effect.getId());
        update.setActive(false);
        update.setConsumedAt(new Date());
        update.setConsumedPingId(ping.getId());
        storage.updateObject(update, new Request(
                new Columns.Include("active", "consumedAt", "consumedPingId"),
                new Condition.Equals("id", effect.getId())));
        cacheManager.invalidateObject(true, GamePendingEffect.class, effect.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, userId, update);

        if (effect.getJokerId() != 0) {
            GameJoker joker = new GameJoker();
            joker.setId(effect.getJokerId());
            joker.setStatus(GameJoker.STATUS_USED);
            joker.setUsedAt(new Date());
            storage.updateObject(joker, new Request(
                    new Columns.Include("status", "usedAt"),
                    new Condition.Equals("id", effect.getJokerId())));
            cacheManager.invalidateObject(true, GameJoker.class, effect.getJokerId(), ObjectOperation.UPDATE);
            actionLogger.edit(request, userId, joker);
        }
    }

    private void applyPosition(Game game, GamePing ping, Position position, String source) {
        if (isPositionTooOld(game, position)) {
            throw new IllegalArgumentException("Latest target position is too old");
        }
        applyPosition(ping, position, source);
    }

    public void applyPosition(GamePing ping, Position position, String source) {
        ping.setSource(source);
        ping.setPositionId(position.getId());
        ping.setFixTime(position.getFixTime());
        ping.setLatitude(position.getLatitude());
        ping.setLongitude(position.getLongitude());
        ping.setAccuracy(position.getAccuracy());
    }

    private boolean isPositionTooOld(Game game, Position position) {
        return game.getMaxPositionAgeSeconds() > 0
                && (position.getFixTime() == null
                        || position.getFixTime().before(
                                new Date(System.currentTimeMillis() - game.getMaxPositionAgeSeconds() * 1000L)));
    }

    private void applyFakePing(GamePing ping, GamePendingEffect effect) {
        try {
            JsonNode payload = objectMapper.readTree(effect.getPayload());
            if (!payload.has("latitude") || !payload.has("longitude")) {
                throw new IllegalArgumentException("Fake ping payload requires latitude and longitude");
            }
            ping.setSource(GamePing.SOURCE_FAKE);
            ping.setLatitude(payload.get("latitude").asDouble());
            ping.setLongitude(payload.get("longitude").asDouble());
            if (payload.has("accuracy")) {
                ping.setAccuracy(payload.get("accuracy").asDouble());
            }
            ping.setFixTime(new Date());
            ping.setConsumedJokerId(effect.getJokerId());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid fake ping payload", e);
        }
    }

}
