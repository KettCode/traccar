package org.traccar.game.state;

import jakarta.inject.Inject;
import org.traccar.game.GameRuntimeContext;
import org.traccar.game.GameRuntimePermissionService;
import org.traccar.game.state.view.GameStateView;
import org.traccar.model.Game;
import org.traccar.model.GameCatch;
import org.traccar.model.GameGeofence;
import org.traccar.model.GameJoker;
import org.traccar.model.GameMember;
import org.traccar.model.GamePendingEffect;
import org.traccar.model.GamePing;
import org.traccar.model.GameReveal;
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
    private static final String INCLUDE_SPEEDHUNT = "speedhunt";
    private static final String INCLUDE_SPEEDHUNT_HISTORY = "speedhuntHistory";
    private static final String INCLUDE_JOKERS = "jokers";
    private static final String INCLUDE_REVEALS = "reveals";
    private static final String INCLUDE_CATCHES = "catches";
    private static final String INCLUDE_GEOFENCES = "geofences";

    @Inject
    private Storage storage;

    @Inject
    private GameRuntimePermissionService runtimePermissionService;

    public GameStateView getState(long userId, long gameId, String include) throws StorageException {
        GameRuntimeContext context = runtimePermissionService.requireRunningMember(userId, gameId);
        if (context == null) {
            return null;
        }

        Set<String> includes = parseIncludes(include);
        Game game = context.game();
        List<GameMember> members = getMembers(gameId);
        Map<Long, GameMember> membersById = indexMembers(members);
        List<GameSpeedhunt> speedhunts = getSpeedhunts(gameId);
        GameSpeedhunt activeSpeedhunt = getActiveSpeedhunt(speedhunts);
        List<GamePing> activeSpeedhuntPings = activeSpeedhunt != null
                ? getSpeedhuntPings(gameId, activeSpeedhunt.getId()) : List.of();

        GameStateView state = new GameStateView();
        state.setGame(toGameView(game));
        state.setCurrentMember(toMemberView(context.member()));
        state.setSummary(toSummaryView(game, members, speedhunts, activeSpeedhunt, activeSpeedhuntPings));
        state.setOwnRuntime(toOwnRuntimeView(context, activeSpeedhunt));
        state.setAllowedActions(toAllowedActionsView(context));

        if (includes.contains(INCLUDE_MEMBERS)) {
            state.setMembers(members.stream().map(this::toMemberView).toList());
        }
        if (includes.contains(INCLUDE_SPEEDHUNT)) {
            state.setSpeedhunt(activeSpeedhunt != null
                    ? toSpeedhuntView(context, activeSpeedhunt, activeSpeedhuntPings, membersById) : null);
        }
        if (includes.contains(INCLUDE_SPEEDHUNT_HISTORY)) {
            state.setSpeedhuntHistory(getSpeedhuntHistory(context, speedhunts, membersById));
        }
        if (includes.contains(INCLUDE_JOKERS)) {
            state.setJokers(getJokers(context, membersById));
        }
        if (includes.contains(INCLUDE_REVEALS)) {
            state.setReveals(getReveals(context));
        }
        if (includes.contains(INCLUDE_CATCHES)) {
            state.setCatches(getCatches(gameId, membersById));
        }
        if (includes.contains(INCLUDE_GEOFENCES)) {
            state.setGeofences(getGeofences(context));
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
                result.add(INCLUDE_SPEEDHUNT);
                result.add(INCLUDE_SPEEDHUNT_HISTORY);
                result.add(INCLUDE_JOKERS);
                result.add(INCLUDE_REVEALS);
                result.add(INCLUDE_CATCHES);
                result.add(INCLUDE_GEOFENCES);
            } else {
                switch (normalized) {
                    case "members" -> result.add(INCLUDE_MEMBERS);
                    case "speedhunt" -> result.add(INCLUDE_SPEEDHUNT);
                    case "speedhunthistory" -> result.add(INCLUDE_SPEEDHUNT_HISTORY);
                    case "jokers" -> result.add(INCLUDE_JOKERS);
                    case "reveals" -> result.add(INCLUDE_REVEALS);
                    case "catches" -> result.add(INCLUDE_CATCHES);
                    case "geofences" -> result.add(INCLUDE_GEOFENCES);
                    default -> { }
                }
            }
        }
        return result;
    }

    private List<GameMember> getMembers(long gameId) throws StorageException {
        return storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("id")));
    }

    private Map<Long, GameMember> indexMembers(List<GameMember> members) {
        var result = new HashMap<Long, GameMember>();
        for (GameMember member : members) {
            result.put(member.getId(), member);
        }
        return result;
    }

    private List<GameSpeedhunt> getSpeedhunts(long gameId) throws StorageException {
        return storage.getObjects(GameSpeedhunt.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("sequenceNumber")));
    }

    private GameSpeedhunt getActiveSpeedhunt(List<GameSpeedhunt> speedhunts) {
        GameSpeedhunt result = null;
        for (GameSpeedhunt speedhunt : speedhunts) {
            if (speedhunt.getEndedAt() == null) {
                result = speedhunt;
            }
        }
        return result;
    }

    private List<GamePing> getSpeedhuntPings(long gameId, long speedhuntId) throws StorageException {
        return storage.getObjects(GamePing.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("speedhuntId", speedhuntId)), new Order("sequenceNumber")));
    }

    private GameStateView.GameView toGameView(Game game) {
        var view = new GameStateView.GameView();
        view.setId(game.getId());
        view.setName(game.getName());
        view.setStatus(game.getStatus());
        view.setStartedAt(game.getStartedAt());
        view.setPlannedEndAt(game.getPlannedEndAt());
        view.setFinishedAt(game.getFinishedAt());
        view.setRemainingSeconds(getRemainingSeconds(game.getPlannedEndAt()));
        return view;
    }

    private Long getRemainingSeconds(Date plannedEndAt) {
        if (plannedEndAt == null) {
            return null;
        }
        return Math.max(0, Duration.between(new Date().toInstant(), plannedEndAt.toInstant()).getSeconds());
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

    private GameStateView.SummaryView toSummaryView(
            Game game, List<GameMember> members, List<GameSpeedhunt> speedhunts,
            GameSpeedhunt activeSpeedhunt, List<GamePing> activeSpeedhuntPings) {
        var view = new GameStateView.SummaryView();
        Date nextRegularPingAt = getNextRegularPingAt(game);
        view.setNextRegularPingAt(nextRegularPingAt);
        view.setNextRegularPingInSeconds(nextRegularPingAt != null ? getRemainingSeconds(nextRegularPingAt) : null);
        for (GameMember member : members) {
            if (GameMember.ROLE_HUNTER.equals(member.getRole())
                    && GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
                view.setActiveHunters(view.getActiveHunters() + 1);
            } else if (GameMember.ROLE_HUNTED.equals(member.getRole())) {
                if (GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
                    view.setActiveHunted(view.getActiveHunted() + 1);
                } else if (GameMember.STATUS_CAUGHT.equals(member.getStatus())) {
                    view.setCaughtHunted(view.getCaughtHunted() + 1);
                }
            }
        }
        view.setSpeedhuntsRemaining(Math.max(0, game.getSpeedhuntLimit() - speedhunts.size()));
        view.setSpeedhuntActive(activeSpeedhunt != null);
        if (activeSpeedhunt != null) {
            view.setSpeedhuntPingNumber(activeSpeedhuntPings.size());
            view.setSpeedhuntPingLimit(activeSpeedhunt.getMaxPings());
        }
        return view;
    }

    private Date getNextRegularPingAt(Game game) {
        if (game.getStartedAt() == null || game.getPingIntervalSeconds() <= 0) {
            return null;
        }

        long intervalMillis = game.getPingIntervalSeconds() * 1000L;
        long startMillis = game.getStartedAt().getTime();
        long nowMillis = System.currentTimeMillis();
        if (nowMillis <= startMillis) {
            return game.getStartedAt();
        }

        long elapsed = nowMillis - startMillis;
        long nextOffset = ((elapsed / intervalMillis) + 1) * intervalMillis;
        return new Date(startMillis + nextOffset);
    }

    private GameStateView.OwnRuntimeView toOwnRuntimeView(
            GameRuntimeContext context, GameSpeedhunt activeSpeedhunt) throws StorageException {
        var view = new GameStateView.OwnRuntimeView();
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
        view.setVisibleSpeedhuntTarget(activeSpeedhunt != null
                && runtimePermissionService.canViewSpeedhuntTarget(context, activeSpeedhunt));
        return view;
    }

    private GameStateView.AllowedActionsView toAllowedActionsView(GameRuntimeContext context) {
        var view = new GameStateView.AllowedActionsView();
        view.setCanStartSpeedhunt(runtimePermissionService.canStartSpeedhunt(context));
        view.setCanRequestSpeedhuntPing(runtimePermissionService.canRequestSpeedhuntPing(context));
        view.setCanUseJoker(context.isActive() && (context.isGameManagement() || context.isHunted()));
        view.setCanManageRuntime(context.isActive() && context.isGameManagement());
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

        boolean targetVisible = runtimePermissionService.canViewSpeedhuntTarget(context, speedhunt);
        view.setTargetRevealed(targetVisible);
        if (targetVisible) {
            GameMember target = membersById.get(speedhunt.getTargetMemberId());
            view.setTargetMemberId(speedhunt.getTargetMemberId());
            view.setTargetDisplayName(target != null ? target.getDisplayName() : null);
        }
        return view;
    }

    private List<GameStateView.SpeedhuntView> getSpeedhuntHistory(
            GameRuntimeContext context, List<GameSpeedhunt> speedhunts,
            Map<Long, GameMember> membersById) throws StorageException {
        if (!context.isGameManagement()) {
            return List.of();
        }

        var result = new ArrayList<GameStateView.SpeedhuntView>();
        for (GameSpeedhunt speedhunt : speedhunts) {
            result.add(toSpeedhuntView(
                    context, speedhunt, getSpeedhuntPings(context.game().getId(), speedhunt.getId()), membersById));
        }
        return result;
    }

    private List<GameStateView.JokerView> getJokers(
            GameRuntimeContext context, Map<Long, GameMember> membersById) throws StorageException {
        List<GameJoker> jokers = context.isGameManagement()
                ? storage.getObjects(GameJoker.class, new Request(
                        new Columns.All(), new Condition.Equals("gameId", context.game().getId()), new Order("id")))
                : getMemberJokers(context.game().getId(), context.member().getId());

        var result = new ArrayList<GameStateView.JokerView>();
        for (GameJoker joker : jokers) {
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

    private List<GameStateView.RevealView> getReveals(GameRuntimeContext context) throws StorageException {
        List<GameReveal> reveals = context.isGameManagement()
                ? storage.getObjects(GameReveal.class, new Request(
                        new Columns.All(), new Condition.Equals("gameId", context.game().getId()), new Order("id")))
                : storage.getObjects(GameReveal.class, new Request(
                        new Columns.All(), new Condition.And(
                                new Condition.Equals("gameId", context.game().getId()),
                                new Condition.Equals("memberId", context.member().getId())), new Order("id")));

        var result = new ArrayList<GameStateView.RevealView>();
        for (GameReveal reveal : reveals) {
            if (runtimePermissionService.canViewReveal(context, reveal)) {
                result.add(toRevealView(reveal));
            }
        }
        return result;
    }

    private GameStateView.RevealView toRevealView(GameReveal reveal) {
        var view = new GameStateView.RevealView();
        view.setId(reveal.getId());
        view.setType(reveal.getType());
        view.setSpeedhuntId(reveal.getSpeedhuntId());
        view.setPayload(reveal.getPayload());
        view.setRevealedAt(reveal.getRevealedAt());
        view.setInvalidatedAt(reveal.getInvalidatedAt());
        return view;
    }

    private List<GameStateView.CatchView> getCatches(
            long gameId, Map<Long, GameMember> membersById) throws StorageException {
        var catches = storage.getObjects(GameCatch.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", gameId), new Order("caughtAt")));
        var result = new ArrayList<GameStateView.CatchView>();
        for (GameCatch catchItem : catches) {
            var view = new GameStateView.CatchView();
            GameMember caughtMember = membersById.get(catchItem.getCaughtMemberId());
            view.setId(catchItem.getId());
            view.setCaughtMemberId(catchItem.getCaughtMemberId());
            view.setCaughtDisplayName(caughtMember != null ? caughtMember.getDisplayName() : null);
            view.setStatus(catchItem.getStatus());
            view.setCaughtAt(catchItem.getCaughtAt());
            view.setRevertedAt(catchItem.getRevertedAt());
            result.add(view);
        }
        return result;
    }

    private List<GameStateView.GeofenceView> getGeofences(GameRuntimeContext context) throws StorageException {
        var geofences = storage.getObjects(GameGeofence.class, new Request(
                new Columns.All(), new Condition.Equals("gameId", context.game().getId()), new Order("id")));
        var result = new ArrayList<GameStateView.GeofenceView>();
        for (GameGeofence geofence : geofences) {
            if (runtimePermissionService.canViewGeofence(context, geofence)) {
                var view = new GameStateView.GeofenceView();
                view.setId(geofence.getId());
                view.setGeofenceId(geofence.getGeofenceId());
                view.setName(geofence.getName());
                view.setType(geofence.getType());
                view.setRole(geofence.getRole());
                result.add(view);
            }
        }
        return result;
    }

}
