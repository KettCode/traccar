package org.traccar.game.map;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.view.GameMapGeofence;
import org.traccar.game.map.view.GameMapMarker;
import org.traccar.game.map.view.GameMapRevealMarker;
import org.traccar.game.map.view.GameMapView;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;
import org.traccar.model.GameRevealedPosition;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameMapService {

    private static final String INCLUDE_REVEALS = "reveals";

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameMapMapper mapMapper;

    public GameMapView getMap(long userId, long gameId, String include) throws StorageException {
        GameRuntimeContext context = runtimePermissionService.requireViewableMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (!context.isRunning()) {
            return getLatestPositionMap(context, gameId);
        }
        return getRunningMap(context, gameId, include);
    }

    private GameMapView getRunningMap(
            GameRuntimeContext context, long gameId, String include) throws StorageException {
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

    private GameMapView getLatestPositionMap(GameRuntimeContext context, long gameId) throws StorageException {
        GameMapView view = new GameMapView();
        view.setGameId(gameId);

        List<GameMember> members = gameStorage.getNonLeftGameMembers(gameId);
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(members);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(
                playersById.values().stream()
                        .map(Player::getDeviceId)
                        .filter(deviceId -> deviceId != 0)
                        .collect(Collectors.toSet()));

        for (GameMember member : members) {
            Player player = playersById.get(member.getPlayerId());
            Position position = player != null ? latestPositionsByDeviceId.get(player.getDeviceId()) : null;
            GameMapMarker marker = mapMapper.toLiveMarker(gameId, member, player, position);
            if (marker != null) {
                view.getMemberMarkers().add(marker);
            }
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

    private List<GameMapMarker> getMemberMarkers(GameRuntimeContext context, List<GameMember> members) throws StorageException {
        List<GameMember> pingMembers = members.stream()
                .filter(member -> runtimePermissionService.canViewPingMapMember(context, member))
                .toList();
        Map<Long, GamePing> latestPingsByMember = context.isActive() && context.isHunter()
                ? gameStorage.getLastVisiblePingsByMembers(pingMembers) : Map.of();
        List<GameMember> liveMembers = members.stream()
                .filter(member -> runtimePermissionService.canViewLiveMapMember(context, member))
                .toList();
        Map<Long, Player> playersById = gameStorage.getPlayersByMembers(liveMembers);
        Map<Long, Position> latestPositionsByDeviceId = gameStorage.getLatestPositionsByDeviceIds(playersById.values().stream()
                .map(Player::getDeviceId)
                .filter(deviceId -> deviceId != 0)
                .collect(Collectors.toSet()));

        var result = new ArrayList<GameMapMarker>();
        for (GameMember member : members) {
            GameMapMarker marker = null;
            if (runtimePermissionService.canViewLiveMapMember(context, member)) {
                Player player = playersById.get(member.getPlayerId());
                Position position = player != null ? latestPositionsByDeviceId.get(player.getDeviceId()) : null;
                marker = mapMapper.toLiveMarker(context.game().getId(), member, player, position);
            } else if (runtimePermissionService.canViewPingMapMember(context, member)) {
                GamePing ping = latestPingsByMember.get(member.getId());
                if (ping != null) {
                    marker = mapMapper.toPingMarker(ping, member);
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
        var gameGeofences = gameStorage.getGameGeofences(context.game().getId());
        var geofencesById = gameStorage.getGeofencesByGameGeofences(gameGeofences);
        for (GameGeofence gameGeofence : gameGeofences) {
            if (runtimePermissionService.canViewGeofence(context, gameGeofence)) {
                GameMapGeofence geofence = mapMapper.toGeofence(
                        gameGeofence, geofencesById.get(gameGeofence.getGeofenceId()));
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
        GameReveal reveal = getLatestOwnHunterLocationReveal(context);
        if (reveal == null) {
            return result;
        }

        for (GameRevealedPosition revealedPosition : gameStorage.getRevealedPositions(reveal.getId())) {
            GameMember member = membersById.get(revealedPosition.getMemberId());
            if (member != null) {
                result.add(mapMapper.toRevealMarker(reveal, revealedPosition, member));
            }
        }
        return result;
    }

    private GameReveal getLatestOwnHunterLocationReveal(GameRuntimeContext context) throws StorageException {
        if (context.isGameManagement()) {
            return null;
        }

        GameReveal reveal = gameStorage.getLatestHunterLocationReveal(context.game().getId(), context.member().getId());
        if (reveal != null && runtimePermissionService.canViewReveal(context, reveal)) {
            return reveal;
        }
        return null;
    }

    private Map<Long, GameMember> indexMembers(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            result.put(member.getId(), member);
        }
        return result;
    }

}
