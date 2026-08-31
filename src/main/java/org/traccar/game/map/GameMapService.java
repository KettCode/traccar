package org.traccar.game.map;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.map.view.GameMapGeofence;
import org.traccar.game.map.view.GameMapMarker;
import org.traccar.game.map.view.GameMapView;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.Player;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameMapService {

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    @Inject
    private GameMapMapper mapMapper;

    public GameMapView getMap(long userId, long gameId) throws StorageException {
        GameRuntimeContext context = runtimePermissionService.requireViewableMember(userId, gameId);
        if (context == null) {
            return null;
        }
        if (Game.STATUS_DRAFT.equals(context.game().getStatus())) {
            return getDraftMap(context, gameId);
        }
        if (!context.isRunning()) {
            return getLatestPositionMap(context, gameId);
        }
        return getRunningMap(context, gameId);
    }

    private GameMapView getRunningMap(GameRuntimeContext context, long gameId) throws StorageException {
        List<GameMember> members = gameStorage.getGameMembers(gameId);

        GameMapView view = new GameMapView();
        view.setGameId(gameId);
        view.getMemberMarkers().addAll(getMemberMarkers(context, members));
        view.getKnowledgeMarkers().addAll(getKnowledgeMarkers(context));
        view.getGeofences().addAll(getGeofences(context));
        return view;
    }

    private GameMapView getDraftMap(GameRuntimeContext context, long gameId) throws StorageException {
        GameMapView view = new GameMapView();
        view.setGameId(gameId);

        Player player = context.player();
        Position position = player.getDeviceId() != 0
                ? gameStorage.getLatestPositionByDeviceId(player.getDeviceId()) : null;
        GameMapMarker marker = mapMapper.toLiveMarker(gameId, context.member(), player, position);
        if (marker != null) {
            view.getMemberMarkers().add(marker);
        }
        view.getGeofences().addAll(getGeofences(context));
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

    private List<GameMapMarker> getKnowledgeMarkers(GameRuntimeContext context) throws StorageException {
        if (!context.isActive() || !context.isHunted()) {
            return List.of();
        }

        GamePing ping = gameStorage.getLatestRegularPing(context.game().getId(), context.member().getId());
        if (ping == null) {
            return List.of();
        }

        GameMapMarker marker = mapMapper.toKnownRegularPingMarker(ping, context.member());
        return marker != null ? List.of(marker) : List.of();
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

}
