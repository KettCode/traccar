package org.traccar.game.map;

import org.traccar.game.map.view.GameMapGeofence;
import org.traccar.game.map.view.GameMapMarker;
import org.traccar.game.map.view.GameMapRevealMarker;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameMember;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;
import org.traccar.model.GameRevealedPosition;
import org.traccar.model.Geofence;
import org.traccar.model.Player;
import org.traccar.model.Position;

public class GameMapMapper {

    private static final String SOURCE_LIVE = "live";
    public static final String SOURCE_KNOWN_REGULAR_PING = "known_regular_ping";

    public GameMapMarker toLiveMarker(long gameId, GameMember member, Player player, Position position) {
        if (player == null || player.getDeviceId() == 0 || position == null) {
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

    public GameMapMarker toPingMarker(GamePing ping, GameMember member) {
        if (ping.getSkipped()) {
            return null;
        }

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

    public GameMapMarker toKnownRegularPingMarker(GamePing ping, GameMember member) {
        GameMapMarker marker = toPingMarker(ping, member);
        if (marker != null) {
            marker.setSource(SOURCE_KNOWN_REGULAR_PING);
            marker.setDeviceId(null);
        }
        return marker;
    }

    public GameMapRevealMarker toRevealMarker(
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

    public GameMapGeofence toGeofence(GameGeofence gameGeofence, Geofence geofence) {
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

    private GameMapMarker createMarker(long gameId, GameMember member) {
        GameMapMarker marker = new GameMapMarker();
        marker.setGameId(gameId);
        marker.setMemberId(member.getId());
        marker.setDisplayName(member.getDisplayName());
        marker.setRole(member.getRole());
        marker.setStatus(member.getStatus());
        return marker;
    }

    private String getClientPingSource(GamePing ping) {
        if (ping.getSpeedhuntId() != 0) {
            return GamePing.SOURCE_SPEEDHUNT;
        }
        return GamePing.SOURCE_REGULAR;
    }

}
