package org.traccar.game;

import org.traccar.game.view.GameLookupOption;
import org.traccar.game.view.GameLookups;
import org.traccar.model.Game;
import org.traccar.model.GameCatch;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;

import java.util.List;

public class GameLookupService {

    private static final String GEOFENCE_STATUS_ACTIVE = "active";
    private static final String GEOFENCE_STATUS_INACTIVE = "inactive";

    public GameLookups getLookups() {
        GameLookups lookups = new GameLookups();
        lookups.setGameStatuses(getGameStatuses());
        lookups.setMemberRoles(getMemberRoles());
        lookups.setMemberStatuses(getMemberStatuses());
        lookups.setGeofenceTypes(getGeofenceTypes());
        lookups.setGeofenceStatuses(getGeofenceStatuses());
        lookups.setJokerTypes(getJokerTypes());
        lookups.setJokerStatuses(getJokerStatuses());
        lookups.setPingSources(getPingSources());
        lookups.setRevealTypes(getRevealTypes());
        lookups.setCatchStatuses(getCatchStatuses());
        lookups.setPendingEffectTypes(getPendingEffectTypes());
        return lookups;
    }

    public List<GameLookupOption> getGameStatuses() {
        return List.of(
                option(Game.STATUS_DRAFT, "Entwurf"),
                option(Game.STATUS_RUNNING, "Läuft"),
                option(Game.STATUS_FINISHED, "Beendet"));
    }

    public List<GameLookupOption> getMemberRoles() {
        return List.of(
                option(GameMember.ROLE_HUNTER, "Jäger"),
                option(GameMember.ROLE_HUNTED, "Gejagter"),
                option(GameMember.ROLE_GAME_MANAGEMENT, "Spielleitung"));
    }

    public List<GameLookupOption> getMemberStatuses() {
        return List.of(
                option(GameMember.STATUS_ACTIVE, "Aktiv"),
                option(GameMember.STATUS_CAUGHT, "Gefangen"),
                option(GameMember.STATUS_LEFT, "Verlassen"));
    }

    public List<GameLookupOption> getGeofenceTypes() {
        return List.of(
                option(GameGeofence.TYPE_PLAYFIELD, "Spielfeld"),
                option(GameGeofence.TYPE_SAFE_ZONE, "Safe Zone"),
                option(GameGeofence.TYPE_RESTRICTED_ZONE, "Sperrzone"),
                option(GameGeofence.TYPE_EVENT_ZONE, "Eventzone"));
    }

    public List<GameLookupOption> getGeofenceStatuses() {
        return List.of(
                option(GEOFENCE_STATUS_ACTIVE, "Aktiv"),
                option(GEOFENCE_STATUS_INACTIVE, "Inaktiv"));
    }

    public List<GameLookupOption> getJokerTypes() {
        return List.of(
                option(GameJoker.TYPE_SKIP_PING, "Ping überspringen"),
                option(GameJoker.TYPE_REQUEST_HUNTER_LOCATIONS, "Jäger-Standorte anfordern"),
                option(GameJoker.TYPE_REVEAL_SPEEDHUNT, "Speedhunt-Ziel aufdecken"),
                option(GameJoker.TYPE_FAKE_PING, "Fake-Ping"));
    }

    public List<GameLookupOption> getJokerStatuses() {
        return List.of(
                option(GameJoker.STATUS_UNLOCKED, "Freigeschaltet"),
                option(GameJoker.STATUS_ACTIVATED, "Aktiviert"),
                option(GameJoker.STATUS_USED, "Verwendet"),
                option(GameJoker.STATUS_CANCELLED, "Abgebrochen"),
                option(GameJoker.STATUS_EXPIRED, "Abgelaufen"));
    }

    public List<GameLookupOption> getPingSources() {
        return List.of(
                option(GamePing.SOURCE_REGULAR, "Regulärer Ping"),
                option(GamePing.SOURCE_SPEEDHUNT, "Speedhunt-Ping"));
    }

    public List<GameLookupOption> getRevealTypes() {
        return List.of(
                option(GameReveal.TYPE_HUNTER_LOCATIONS, "Jäger-Standorte"),
                option(GameReveal.TYPE_SPEEDHUNT_TARGET, "Speedhunt-Ziel"));
    }

    public List<GameLookupOption> getCatchStatuses() {
        return List.of(
                option(GameCatch.STATUS_ACTIVE, "Aktiv"),
                option(GameCatch.STATUS_REVERTED, "Zurückgenommen"));
    }

    public List<GameLookupOption> getPendingEffectTypes() {
        return List.of(
                option(GamePendingEffect.EFFECT_SKIP_NEXT_PING, "Nächsten Ping überspringen"),
                option(GamePendingEffect.EFFECT_FAKE_NEXT_PING, "Nächsten Ping fälschen"));
    }

    private GameLookupOption option(String value, String label) {
        return new GameLookupOption(value, label);
    }

}
