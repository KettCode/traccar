package org.traccar.game.map;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.view.GameMapView;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;
import org.traccar.model.GameRevealedPosition;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameMapService {

    private static final String SOURCE_LIVE = "live";
    private static final String INCLUDE_REVEALS = "reveals";

    @Inject
    private Storage storage;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    public GameMapView getMap(long userId, long gameId, String include) throws StorageException {
        GameRuntimeContext context = runtimePermissionService.requireRunningMember(userId, gameId);
        if (context == null) {
            return null;
        }
        Set<String> includes = parseIncludes(include);

        List<GameMember> members = gameStorage.getGameMembers(gameId);
        Map<Long, GameMember> membersById = indexMembers(members);

        GameMapView view = new GameMapView();
        view.setGameId(gameId);
        view.getMemberMarkers().addAll(getMemberMarkers(context, members));
        view.getGeofences().addAll(getGeofences(context));
        if (includes.contains(INCLUDE_REVEALS)) {
            view.getRevealedMarkers().addAll(getRevealMarkers(context, membersById));
        }
        return view;
    }

    private Set<String> parseIncludes(String include) {
        Set<String> result = new HashSet<>();
        if (include == null || include.isBlank()) {
            return result;
        }
        for (String rawValue : include.split(",")) {
            String value = rawValue.trim().toLowerCase(Locale.ROOT);
            if (INCLUDE_REVEALS.equals(value) || "all".equals(value)) {
                result.add(INCLUDE_REVEALS);
            }
        }
        return result;
    }

    private List<GameMapMarker> getMemberMarkers(GameRuntimeContext context, List<GameMember> members)
            throws StorageException {
        Map<Long, GamePing> latestPingsByMember = context.isActive() && context.isHunter()
                ? getLatestVisiblePingsByMember(context.game().getId()) : Map.of();
        List<GameMember> liveMembers = members.stream()
                .filter(member -> canViewLiveMember(context, member))
                .toList();
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(liveMembers);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(playersById.values().stream()
                .map(Player::getDeviceId)
                .filter(deviceId -> deviceId != 0)
                .collect(Collectors.toSet()));

        var result = new ArrayList<GameMapMarker>();
        for (GameMember member : members) {
            GameMapMarker marker = null;
            if (canViewLiveMember(context, member)) {
                marker = getLiveMarker(context.game().getId(), member, playersById, latestPositionsByDeviceId);
            } else if (canViewPingMember(context, member)) {
                GamePing ping = latestPingsByMember.get(member.getId());
                if (ping != null) {
                    marker = toPingMarker(ping, member);
                }
            }
            if (marker != null) {
                result.add(marker);
            }
        }
        return result;
    }

    private List<GameMapGeofence> getGeofences(GameRuntimeContext context) throws StorageException {
        var result = new ArrayList<GameMapGeofence>();
        var gameGeofences = storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", context.game().getId()), new Order("id")));
        Map<Long, Geofence> geofencesById = getGeofencesById(gameGeofences);
        for (GameGeofence gameGeofence : gameGeofences) {
            if (runtimePermissionService.canViewGeofence(context, gameGeofence)) {
                GameMapGeofence geofence = toGeofence(gameGeofence, geofencesById.get(gameGeofence.getGeofenceId()));
                if (geofence != null) {
                    result.add(geofence);
                }
            }
        }
        return result;
    }

    private List<GameMapRevealMarker> getRevealMarkers(
            GameRuntimeContext context, Map<Long, GameMember> membersById) throws StorageException {
        var result = new ArrayList<GameMapRevealMarker>();
        List<GameReveal> visibleReveals = getVisibleReveals(context);
        Map<Long, List<GameRevealedPosition>> positionsByRevealId = getRevealPositionsByRevealId(visibleReveals);
        for (GameReveal reveal : visibleReveals) {
            if (!GameReveal.TYPE_HUNTER_LOCATIONS.equals(reveal.getType())) {
                continue;
            }
            for (GameRevealedPosition revealedPosition : positionsByRevealId.getOrDefault(reveal.getId(), List.of())) {
                GameMember member = membersById.get(revealedPosition.getMemberId());
                if (member != null) {
                    result.add(toRevealMarker(reveal, revealedPosition, member));
                }
            }
        }
        return result;
    }

    private List<GameReveal> getVisibleReveals(GameRuntimeContext context) throws StorageException {
        List<GameReveal> reveals = context.isGameManagement()
                ? storage.getObjects(GameReveal.class, new Request(
                        new Columns.All(), new Condition.Equals("gameId", context.game().getId()), new Order("id")))
                : storage.getObjects(GameReveal.class, new Request(
                        new Columns.All(), new Condition.And(
                                new Condition.Equals("gameId", context.game().getId()),
                                new Condition.Equals("memberId", context.member().getId())), new Order("id")));
        return reveals.stream()
                .filter(reveal -> reveal.getInvalidatedAt() == null)
                .filter(reveal -> runtimePermissionService.canViewReveal(context, reveal))
                .toList();
    }

    private GameMapMarker getLiveMarker(
            long gameId, GameMember member, Map<Long, Player> playersById,
            Map<Long, Position> latestPositionsByDeviceId) {
        Player player = playersById.get(member.getPlayerId());
        if (player == null || player.getDeviceId() == 0) {
            return null;
        }

        Position position = latestPositionsByDeviceId.get(player.getDeviceId());
        if (position == null) {
            return null;
        }

        GameMapMarker marker = createMarker(gameId, member);
        marker.setDeviceId(player.getDeviceId());
        marker.setSource(SOURCE_LIVE);
        marker.setPositionId(position.getId());
        marker.setFixTime(position.getFixTime());
        marker.setLatitude(position.getLatitude());
        marker.setLongitude(position.getLongitude());
        marker.setAccuracy(position.getAccuracy());
        return marker;
    }

    private GameMapMarker toPingMarker(GamePing ping, GameMember member) {
        GameMapMarker marker = createMarker(ping.getGameId(), member);
        marker.setSource(getClientPingSource(ping));
        marker.setPingId(ping.getId());
        if (ping.getSpeedhuntId() != 0) {
            marker.setSpeedhuntId(ping.getSpeedhuntId());
        }
        if (ping.getPositionId() != 0) {
            marker.setPositionId(ping.getPositionId());
        }
        marker.setFixTime(ping.getFixTime());
        marker.setLatitude(ping.getLatitude());
        marker.setLongitude(ping.getLongitude());
        marker.setAccuracy(ping.getAccuracy());
        return marker;
    }

    private GameMapMarker createMarker(long gameId, GameMember member) {
        GameMapMarker marker = new GameMapMarker();
        marker.setGameId(gameId);
        marker.setMemberId(member.getId());
        marker.setDisplayName(member.getDisplayName());
        marker.setRole(member.getRole());
        marker.setStatus(member.getStatus());
        return marker;
    }

    private GameMapRevealMarker toRevealMarker(
            GameReveal reveal, GameRevealedPosition revealedPosition, GameMember member) {
        GameMapRevealMarker marker = new GameMapRevealMarker();
        marker.setRevealId(reveal.getId());
        marker.setMemberId(member.getId());
        marker.setDisplayName(member.getDisplayName());
        marker.setRole(member.getRole());
        marker.setStatus(member.getStatus());
        marker.setSource(reveal.getType());
        if (revealedPosition.getPositionId() != 0) {
            marker.setPositionId(revealedPosition.getPositionId());
        }
        marker.setFixTime(revealedPosition.getFixTime());
        marker.setRevealedAt(reveal.getRevealedAt());
        marker.setLatitude(revealedPosition.getLatitude());
        marker.setLongitude(revealedPosition.getLongitude());
        marker.setAccuracy(revealedPosition.getAccuracy());
        return marker;
    }

    private GameMapGeofence toGeofence(GameGeofence gameGeofence, Geofence geofence) {
        if (geofence == null) {
            return null;
        }

        GameMapGeofence view = new GameMapGeofence();
        view.setId(gameGeofence.getId());
        view.setGameId(gameGeofence.getGameId());
        view.setGeofenceId(gameGeofence.getGeofenceId());
        view.setName(gameGeofence.getName());
        view.setType(gameGeofence.getType());
        view.setRole(gameGeofence.getRole());
        view.setArea(geofence.getArea());
        return view;
    }

    private Map<Long, GamePing> getLatestVisiblePingsByMember(long gameId) throws StorageException {
        var result = new HashMap<Long, GamePing>();
        var pings = storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
        for (GamePing ping : pings) {
            if (!ping.getSkipped()) {
                result.put(ping.getTargetMemberId(), ping);
            }
        }
        return result;
    }

    private Map<Long, Geofence> getGeofencesById(List<GameGeofence> gameGeofences) throws StorageException {
        Condition condition = null;
        for (GameGeofence gameGeofence : gameGeofences) {
            condition = addOrEquals(condition, "id", gameGeofence.getGeofenceId());
        }
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, Geofence>();
        var geofences = storage.getObjects(Geofence.class, new Request(new Columns.All(), condition, new Order("id")));
        for (Geofence geofence : geofences) {
            result.put(geofence.getId(), geofence);
        }
        return result;
    }

    private Map<Long, List<GameRevealedPosition>> getRevealPositionsByRevealId(List<GameReveal> reveals)
            throws StorageException {
        Condition condition = null;
        for (GameReveal reveal : reveals) {
            if (GameReveal.TYPE_HUNTER_LOCATIONS.equals(reveal.getType())) {
                condition = addOrEquals(condition, "revealId", reveal.getId());
            }
        }
        if (condition == null) {
            return Map.of();
        }

        var result = new HashMap<Long, List<GameRevealedPosition>>();
        var positions = storage.getObjects(GameRevealedPosition.class, new Request(
                new Columns.All(), condition, new Order("id")));
        for (GameRevealedPosition position : positions) {
            result.computeIfAbsent(position.getRevealId(), key -> new ArrayList<>()).add(position);
        }
        return result;
    }

    private String getClientPingSource(GamePing ping) {
        if (ping.getSpeedhuntId() != 0) {
            return GamePing.SOURCE_SPEEDHUNT;
        }
        return GamePing.SOURCE_REGULAR;
    }

    private boolean canViewLiveMember(GameRuntimeContext context, GameMember member) {
        if (!context.isActive() || !GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            return false;
        }
        if (context.isGameManagement()) {
            return true;
        }
        if (context.isHunter()) {
            return GameMember.ROLE_HUNTER.equals(member.getRole())
                    || GameMember.ROLE_GAME_MANAGEMENT.equals(member.getRole());
        }
        return context.isHunted() && GameMember.ROLE_HUNTED.equals(member.getRole());
    }

    private boolean canViewPingMember(GameRuntimeContext context, GameMember member) {
        return context.isActive()
                && context.isHunter()
                && GameMember.STATUS_ACTIVE.equals(member.getStatus())
                && GameMember.ROLE_HUNTED.equals(member.getRole());
    }

    private Map<Long, GameMember> indexMembers(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            result.put(member.getId(), member);
        }
        return result;
    }

    private Condition addOrEquals(Condition condition, String column, long value) {
        Condition equals = new Condition.Equals(column, value);
        if (condition == null) {
            return equals;
        }
        return new Condition.Or(condition, equals);
    }

}
