package org.traccar.game.state;

import jakarta.inject.Inject;
import org.traccar.game.GameStorage;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.state.view.GameStateView;
import org.traccar.model.Game;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.GameSpeedhunt;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GameStateService {

    private static final String INCLUDE_MEMBERS = "members";
    private static final String INCLUDE_GEOFENCES = "geofences";
    private static final String INCLUDE_SPEEDHUNT_HISTORY = "speedhuntHistory";
    private static final String INCLUDE_JOKERS = "jokers";

    @Inject
    private Storage storage;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    @Inject
    private GameStorage gameStorage;

    public GameStateView getState(long userId, long gameId, String include) throws StorageException {
        GameRuntimeContext context = runtimePermissionService.requireViewableMember(userId, gameId);
        if (context == null) {
            return null;
        }

        Set<String> includes = parseIncludes(include);
        Date now = new Date();
        Game game = context.game();
        List<GameMember> members = gameStorage.getGameMembers(gameId);
        Map<Long, GameMember> membersById = indexMembers(members);
        List<GameSpeedhunt> speedhunts = gameStorage.getGameSpeedhunts(gameId);
        GameSpeedhunt activeSpeedhunt = getActiveSpeedhunt(speedhunts);
        boolean includeSpeedhuntHistory = includes.contains(INCLUDE_SPEEDHUNT_HISTORY) && context.isGameManagement();
        Map<Long, List<GamePing>> pingsBySpeedhuntId = includeSpeedhuntHistory
                ? getSpeedhuntPingsGroupedBySpeedhuntId(gameId) : Map.of();
        List<GamePing> activeSpeedhuntPings = List.of();
        if (activeSpeedhunt != null) {
            activeSpeedhuntPings = includeSpeedhuntHistory
                    ? pingsBySpeedhuntId.getOrDefault(activeSpeedhunt.getId(), List.of())
                    : getSpeedhuntPings(gameId, activeSpeedhunt.getId());
        }

        GameStateView state = new GameStateView();
        state.setGame(toGameView(game, now));
        state.setCurrentMember(toMemberView(context.member()));
        state.setSummary(toSummaryView(
                context, game, speedhunts, activeSpeedhunt, activeSpeedhuntPings, membersById, now));
        state.setAllowedActions(toAllowedActionsView(context));

        if (includes.contains(INCLUDE_MEMBERS)) {
            state.setMembers(members.stream().map(this::toMemberView).toList());
        }
        if (includes.contains(INCLUDE_GEOFENCES)) {
            state.setGeofences(getGeofences(context));
        }
        if (includeSpeedhuntHistory) {
            state.setSpeedhuntHistory(getSpeedhuntHistory(context, speedhunts, membersById, pingsBySpeedhuntId));
        }
        if (includes.contains(INCLUDE_JOKERS)) {
            state.setJokers(getJokers(context, membersById));
        }

        return state;
    }

    private Set<String> parseIncludes(String include) {
        if (include == null || include.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        for (String rawValue : include.split(",")) {
            String value = rawValue.trim();
            if (value.isEmpty()) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if ("all".equals(normalized) || "management".equals(normalized)) {
                result.add(INCLUDE_MEMBERS);
                result.add(INCLUDE_GEOFENCES);
                result.add(INCLUDE_SPEEDHUNT_HISTORY);
                result.add(INCLUDE_JOKERS);
            } else {
                switch (normalized) {
                    case "members" -> result.add(INCLUDE_MEMBERS);
                    case "geofences" -> result.add(INCLUDE_GEOFENCES);
                    case "speedhunthistory" -> result.add(INCLUDE_SPEEDHUNT_HISTORY);
                    case "jokers" -> result.add(INCLUDE_JOKERS);
                    default -> { }
                }
            }
        }
        return result;
    }

    private Map<Long, GameMember> indexMembers(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            result.put(member.getId(), member);
        }
        return result;
    }

    private GameSpeedhunt getActiveSpeedhunt(List<GameSpeedhunt> speedhunts) {
        for (GameSpeedhunt speedhunt : speedhunts) {
            if (speedhunt.getEndedAt() == null) {
                return speedhunt;
            }
        }
        return null;
    }

    private List<GamePing> getSpeedhuntPings(long gameId, long speedhuntId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("speedhuntId", speedhuntId)), new Order("sequenceNumber")));
    }

    private Map<Long, List<GamePing>> getSpeedhuntPingsGroupedBySpeedhuntId(long gameId) throws StorageException {
        var result = new HashMap<Long, List<GamePing>>();
        for (GamePing ping : gameStorage.getSpeedhuntPings(gameId)) {
            result.computeIfAbsent(ping.getSpeedhuntId(), key -> new ArrayList<>()).add(ping);
        }
        return result;
    }

    private GameStateView.GameView toGameView(Game game, Date now) {
        var view = new GameStateView.GameView();
        view.setId(game.getId());
        view.setName(game.getName());
        view.setStatus(game.getStatus());
        view.setStartedAt(game.getStartedAt());
        view.setActivatedAt(game.getActivatedAt());
        view.setPlannedEndAt(game.getPlannedEndAt());
        view.setFinishedAt(game.getFinishedAt());
        view.setRemainingSeconds(getRemainingSeconds(game.getPlannedEndAt(), now));
        view.setPingIntervalSeconds(game.getPingIntervalSeconds());
        view.setSpeedhuntLimit(game.getSpeedhuntLimit());
        view.setSpeedhuntPingLimit(game.getSpeedhuntPingLimit());
        view.setAllowConsecutiveSpeedhuntsSameTarget(game.getAllowConsecutiveSpeedhuntsSameTarget());
        view.setLocationReminderEnabled(game.getLocationReminderEnabled());
        view.setMaxPositionAgeSeconds(game.getMaxPositionAgeSeconds());
        view.setLocationReminderIntervalSeconds(game.getLocationReminderIntervalSeconds());
        return view;
    }

    private Long getRemainingSeconds(Date plannedEndAt, Date now) {
        if (plannedEndAt == null) {
            return null;
        }
        return Math.max(0, Duration.between(now.toInstant(), plannedEndAt.toInstant()).getSeconds());
    }

    private GameStateView.MemberView toMemberView(GameMember member) {
        var view = new GameStateView.MemberView();
        view.setId(member.getId());
        view.setDisplayName(member.getDisplayName());
        view.setRole(member.getRole());
        view.setStatus(member.getStatus());
        view.setCaughtAt(member.getCaughtAt());
        return view;
    }

    private List<GameStateView.GeofenceView> getGeofences(GameRuntimeContext context) throws StorageException {
        if (!context.isGameManagement()) {
            return List.of();
        }

        return gameStorage.getGameGeofences(context.game().getId()).stream()
                .map(this::toGeofenceView)
                .toList();
    }

    private GameStateView.GeofenceView toGeofenceView(GameGeofence geofence) {
        var view = new GameStateView.GeofenceView();
        view.setId(geofence.getId());
        view.setName(geofence.getName());
        view.setType(geofence.getType());
        view.setRole(geofence.getRole());
        view.setActive(geofence.getActive());
        return view;
    }

    private GameStateView.SummaryView toSummaryView(
            GameRuntimeContext context, Game game, List<GameSpeedhunt> speedhunts,
            GameSpeedhunt activeSpeedhunt, List<GamePing> activeSpeedhuntPings,
            Map<Long, GameMember> membersById, Date now) throws StorageException {
        var view = new GameStateView.SummaryView();
        Date nextRegularPingAt = getNextRegularPingAt(game, now);
        view.setNextRegularPingAt(nextRegularPingAt);
        view.setNextRegularPingInSeconds(nextRegularPingAt != null ? getRemainingSeconds(nextRegularPingAt, now) : null);
        view.setSpeedhuntsRemaining(Math.max(0, game.getSpeedhuntLimit() - speedhunts.size()));
        view.setSpeedhuntActive(activeSpeedhunt != null);
        if (activeSpeedhunt != null) {
            view.setSpeedhuntId(activeSpeedhunt.getId());
            view.setSpeedhuntPingNumber(activeSpeedhuntPings.size());
            view.setSpeedhuntPingLimit(activeSpeedhunt.getMaxPings());
            boolean targetVisible = runtimePermissionService.canViewSpeedhuntTarget(context, activeSpeedhunt);
            view.setSpeedhuntTargetRevealed(targetVisible);
            if (targetVisible) {
                GameMember target = membersById.get(activeSpeedhunt.getTargetMemberId());
                view.setSpeedhuntTargetMemberId(activeSpeedhunt.getTargetMemberId());
                view.setSpeedhuntTargetDisplayName(target != null ? target.getDisplayName() : null);
            }
        }
        if (context.isHunted()) {
            applyOwnJokerSummary(context, view);
        }
        return view;
    }

    private Date getNextRegularPingAt(Game game, Date now) {
        if (game.getStartedAt() == null || game.getPingIntervalSeconds() <= 0) {
            return null;
        }

        long intervalMillis = game.getPingIntervalSeconds() * 1000L;
        long startMillis = game.getStartedAt().getTime();
        long nowMillis = now.getTime();
        if (nowMillis <= startMillis) {
            return game.getStartedAt();
        }

        long elapsed = nowMillis - startMillis;
        long nextOffset = ((elapsed / intervalMillis) + 1) * intervalMillis;
        return new Date(startMillis + nextOffset);
    }

    private void applyOwnJokerSummary(GameRuntimeContext context, GameStateView.SummaryView view)
            throws StorageException {
        List<GameJoker> ownJokers = getMemberJokers(context.game().getId(), context.member().getId());
        List<GamePendingEffect> activeEffects = storage.getObjects(GamePendingEffect.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.And(
                                new Condition.Equals("gameId", context.game().getId()),
                                new Condition.Equals("memberId", context.member().getId())),
                        new Condition.Equals("active", true)), new Order("id")));
        var jokerTypesById = new HashMap<Long, String>();
        for (GameJoker joker : ownJokers) {
            jokerTypesById.put(joker.getId(), joker.getType());
            if (GameJoker.STATUS_ACTIVATED.equals(joker.getStatus())) {
                view.setActiveJokerEffect(true);
            }
        }

        var activeJokerTypes = new ArrayList<String>();
        for (GamePendingEffect effect : activeEffects) {
            view.setActiveJokerEffect(true);
            String type = jokerTypesById.get(effect.getJokerId());
            activeJokerTypes.add(type != null ? type : effect.getEffect());
        }
        view.setActiveJokerTypes(activeJokerTypes);
    }

    private GameStateView.AllowedActionsView toAllowedActionsView(GameRuntimeContext context) {
        var view = new GameStateView.AllowedActionsView();
        view.setCanStartSpeedhunt(runtimePermissionService.canStartSpeedhunt(context));
        view.setCanRequestSpeedhuntPing(runtimePermissionService.canRequestSpeedhuntPing(context));
        view.setCanUseJoker(runtimePermissionService.canUseJoker(context));
        view.setCanManageRuntime(runtimePermissionService.canManageRuntime(context));
        view.setCanManageRuntimeSettings(runtimePermissionService.canManageRuntimeSettings(context));
        view.setCanManageGeofences(runtimePermissionService.canManageGeofences(context));
        view.setCanUnlockJoker(runtimePermissionService.canUnlockJoker(context));
        return view;
    }

    private GameStateView.SpeedhuntView toSpeedhuntView(
            GameRuntimeContext context, GameSpeedhunt speedhunt, List<GamePing> pings,
            Map<Long, GameMember> membersById) throws StorageException {
        var view = new GameStateView.SpeedhuntView();
        view.setActive(speedhunt.getEndedAt() == null);
        view.setId(speedhunt.getId());
        view.setSequenceNumber(speedhunt.getSequenceNumber());
        view.setPingNumber(pings.size());
        view.setMaxPings(speedhunt.getMaxPings());
        view.setStartedAt(speedhunt.getStartedAt());
        view.setEndedAt(speedhunt.getEndedAt());
        view.setPings(pings.stream().map(this::toSpeedhuntPingView).toList());

        boolean targetVisible = runtimePermissionService.canViewSpeedhuntTarget(context, speedhunt);
        view.setTargetRevealed(targetVisible);
        if (targetVisible) {
            GameMember target = membersById.get(speedhunt.getTargetMemberId());
            view.setTargetMemberId(speedhunt.getTargetMemberId());
            view.setTargetDisplayName(target != null ? target.getDisplayName() : null);
        }
        return view;
    }

    private GameStateView.SpeedhuntPingView toSpeedhuntPingView(GamePing ping) {
        var view = new GameStateView.SpeedhuntPingView();
        view.setId(ping.getId());
        view.setSequenceNumber(ping.getSequenceNumber());
        view.setCreatedAt(ping.getCreatedAt());
        view.setFixTime(ping.getFixTime());
        view.setSkipped(ping.getSkipped());
        return view;
    }

    private List<GameStateView.SpeedhuntView> getSpeedhuntHistory(
            GameRuntimeContext context, List<GameSpeedhunt> speedhunts,
            Map<Long, GameMember> membersById, Map<Long, List<GamePing>> pingsBySpeedhuntId) throws StorageException {
        if (!context.isGameManagement()) {
            return List.of();
        }

        var result = new ArrayList<GameStateView.SpeedhuntView>();
        for (GameSpeedhunt speedhunt : speedhunts) {
            result.add(toSpeedhuntView(
                    context, speedhunt, pingsBySpeedhuntId.getOrDefault(speedhunt.getId(), List.of()), membersById));
        }
        return result;
    }

    private List<GameStateView.JokerView> getJokers(
            GameRuntimeContext context, Map<Long, GameMember> membersById) throws StorageException {
        if (!context.isGameManagement() && !context.isHunted()) {
            return List.of();
        }

        Condition condition = new Condition.Equals("gameId", context.game().getId());
        if (!context.isGameManagement()) {
            condition = new Condition.And(condition, new Condition.Equals("memberId", context.member().getId()));
        }

        var result = new ArrayList<GameStateView.JokerView>();
        for (GameJoker joker : storage.getObjects(GameJoker.class, new Request(
                new Columns.All(), condition, new Order("id")))) {
            if (runtimePermissionService.canViewJoker(context, joker)) {
                result.add(toJokerView(context, joker, membersById));
            }
        }
        return result;
    }

    private List<GameJoker> getMemberJokers(long gameId, long memberId) throws StorageException {
        return storage.getObjects(GameJoker.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("memberId", memberId)), new Order("id")));
    }

    private GameStateView.JokerView toJokerView(
            GameRuntimeContext context, GameJoker joker, Map<Long, GameMember> membersById) {
        var view = new GameStateView.JokerView();
        view.setId(joker.getId());
        if (context.isGameManagement()) {
            GameMember member = membersById.get(joker.getMemberId());
            view.setMemberId(joker.getMemberId());
            view.setMemberDisplayName(member != null ? member.getDisplayName() : null);
        }
        view.setType(joker.getType());
        view.setStatus(joker.getStatus());
        view.setUnlockedAt(joker.getUnlockedAt());
        view.setActivatedAt(joker.getActivatedAt());
        view.setUsedAt(joker.getUsedAt());
        view.setCancelledAt(joker.getCancelledAt());
        return view;
    }

}
