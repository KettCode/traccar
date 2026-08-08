package org.traccar.game.setup.wizard;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.game.GamePermissionService;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.game.setup.wizard.request.WizardMemberRequest;
import org.traccar.game.setup.wizard.request.WizardPasswordRequest;
import org.traccar.game.setup.wizard.view.WizardReusablePlayer;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.Game;
import org.traccar.model.GameMember;
import org.traccar.model.ManagedUser;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Player;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;

public class WizardMemberService {

    private static final String GAME_EMAIL_DOMAIN = "@game.local";
    private static final int MAX_USERNAME_LENGTH = 128 - GAME_EMAIL_DOMAIN.length();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    @Inject
    private Storage storage;

    @Inject
    private PermissionsService permissionsService;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GamePermissionService gamePermissionService;

    @Inject
    private GameService gameService;

    @Inject
    private GameValidatorService validator;

    @Inject
    private WizardClientSetupService clientSetupService;

    public boolean addPlayers(
            long userId, long gameId, List<WizardMemberRequest> requests,
            HttpServletRequest httpRequest) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Game players are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        for (WizardMemberRequest request : requests) {
            addPlayer(userId, game, request, httpRequest);
        }
        return true;
    }

    public List<WizardReusablePlayer> getReusablePlayers(long userId, long gameId) throws StorageException {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return null;
        }
        var activePlayerIds = getActivePlayerIds(gameId);
        var conditions = new ArrayList<Condition>();
        conditions.add(new Condition.Equals("active", true));
        if (permissionsService.notAdmin(userId)) {
            conditions.add(new Condition.Permission(User.class, userId, Player.class));
        }

        var result = new ArrayList<WizardReusablePlayer>();
        var players = storage.getObjects(Player.class, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("id")));
        for (Player player : players) {
            if (!activePlayerIds.contains(player.getId())) {
                result.add(toReusablePlayer(player));
            }
        }
        return result;
    }

    public boolean reusePlayers(
            long userId, long gameId, List<GameMember> requests,
            HttpServletRequest httpRequest) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Reusable game players are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        for (GameMember request : requests) {
            reusePlayer(userId, game, request, httpRequest);
        }
        return true;
    }

    public boolean updatePlayer(
            long userId, long gameId, long memberId, GameMember request,
            HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        if (request == null) {
            throw new IllegalArgumentException("Game player is required");
        }

        GameMember member = getGameMember(gameId, memberId);
        if (member == null) {
            return false;
        }
        if (!GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            throw new IllegalArgumentException("Only active setup players can be changed");
        }

        String displayName = request.getDisplayName() != null ? request.getDisplayName().trim() : null;
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }
        String role = request.getRole() != null ? request.getRole().trim() : null;
        validator.validateRole(role);

        GameMember memberUpdate = new GameMember();
        memberUpdate.setId(memberId);
        memberUpdate.setDisplayName(displayName);
        memberUpdate.setRole(role);
        storage.updateObject(memberUpdate, new Request(
                new Columns.Include("displayName", "role"),
                new Condition.Equals("id", memberId)));
        cacheManager.invalidateObject(true, GameMember.class, memberId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, memberUpdate);

        return true;
    }

    public boolean removePlayer(
            long userId, long gameId, long memberId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        GameMember member = getGameMember(gameId, memberId);
        if (member == null) {
            return false;
        }
        Player player = getPlayer(member.getPlayerId());
        if (player == null) {
            throw new IllegalArgumentException("Game player assignment is missing");
        }

        GameMember memberUpdate = new GameMember();
        memberUpdate.setId(memberId);
        memberUpdate.setStatus(GameMember.STATUS_LEFT);
        storage.updateObject(memberUpdate, new Request(
                new Columns.Include("status"),
                new Condition.Equals("id", memberId)));
        cacheManager.invalidateObject(true, GameMember.class, memberId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, memberUpdate);

        return true;
    }

    public boolean updatePassword(
            long userId, long gameId, long memberId, WizardPasswordRequest request,
            HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        if (request == null) {
            throw new IllegalArgumentException("Password is required");
        }

        GameMember member = getGameMember(gameId, memberId);
        if (member == null) {
            return false;
        }
        if (!GameMember.STATUS_ACTIVE.equals(member.getStatus())) {
            throw new IllegalArgumentException("Only active setup players can be changed");
        }

        Player player = getPlayer(member.getPlayerId());
        if (player == null || player.getUserId() == 0) {
            throw new IllegalArgumentException("Game player assignment is missing");
        }

        permissionsService.checkUser(userId, player.getUserId());
        updateUserPassword(userId, player.getUserId(), request.getPassword(), httpRequest);
        return true;
    }

    public void copyActiveMembers(
            long userId, long sourceGameId, Game targetGame,
            HttpServletRequest httpRequest) throws Exception {
        gameService.getEditableDraftGame(userId, targetGame.getId());

        var members = storage.getObjects(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("gameId", sourceGameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE)), new Order("id")));
        for (GameMember member : members) {
            Player player = getPlayer(member.getPlayerId());
            if (player == null || !player.getActive() || player.getUserId() == 0 || player.getDeviceId() == 0) {
                continue;
            }
            validator.validateRole(member.getRole());
            ensurePlayerPermissions(userId, player, httpRequest);
            addGameMember(userId, targetGame, player, member.getDisplayName(), member.getRole(), httpRequest);
        }
    }

    private void addPlayer(
            long userId, Game game, WizardMemberRequest request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Game player is required");
        }

        String displayName = request.getDisplayName() != null ? request.getDisplayName().trim() : null;
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }
        String username = request.getUsername() != null ? request.getUsername().trim() : null;
        validateUsername(username);
        checkUsernameAvailable(username);

        String role = request.getRole() != null ? request.getRole().trim() : null;
        validator.validateRole(role);
        validatePassword(request.getPassword());

        checkAddAccess(userId);

        User playerUser = addUser(userId, username, request.getPassword(), httpRequest);
        Device device = addDevice(userId, username, httpRequest);

        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(httpRequest, userId, userId, ManagedUser.class, playerUser.getId());
            gamePermissionService.addPermission(httpRequest, userId, userId, Device.class, device.getId());
        }
        gamePermissionService.addPermission(httpRequest, userId, playerUser.getId(), Device.class, device.getId());

        Player player = addPlayerObject(userId, playerUser, device, httpRequest);

        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(httpRequest, userId, userId, Player.class, player.getId());
        }
        gamePermissionService.addPermission(httpRequest, userId, playerUser.getId(), Player.class, player.getId());

        addGameMember(userId, game, player, displayName, role, httpRequest);
    }

    private void reusePlayer(
            long userId, Game game, GameMember request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null || request.getPlayerId() == 0) {
            throw new IllegalArgumentException("Player is required");
        }

        permissionsService.checkPermission(Player.class, userId, request.getPlayerId());

        Player player = getPlayer(request.getPlayerId());
        if (player == null || !player.getActive() || player.getUserId() == 0 || player.getDeviceId() == 0) {
            throw new IllegalArgumentException("Player is not reusable");
        }
        if (getActivePlayerIds(game.getId()).contains(player.getId())) {
            throw new IllegalArgumentException("Player is already active in this game");
        }

        String role = request.getRole() != null ? request.getRole().trim() : null;
        validator.validateRole(role);

        String displayName = request.getDisplayName() != null ? request.getDisplayName().trim() : null;
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }

        ensurePlayerPermissions(userId, player, httpRequest);
        addGameMember(userId, game, player, displayName, role, httpRequest);
    }

    private void ensurePlayerPermissions(
            long userId, Player player, HttpServletRequest httpRequest) throws Exception {
        if (userId != ServiceAccountUser.ID) {
            gamePermissionService.addPermission(httpRequest, userId, userId, Player.class, player.getId());
            gamePermissionService.addPermission(httpRequest, userId, userId, Device.class, player.getDeviceId());
            gamePermissionService.addPermission(httpRequest, userId, userId, ManagedUser.class, player.getUserId());
        }
        gamePermissionService.addPermission(httpRequest, userId, player.getUserId(), Player.class, player.getId());
        gamePermissionService.addPermission(httpRequest, userId, player.getUserId(), Device.class, player.getDeviceId());
    }

    private void checkAddAccess(long userId) throws StorageException {
        permissionsService.checkEdit(userId, User.class, true, false);
        permissionsService.checkEdit(userId, Device.class, true, false);
        permissionsService.checkEdit(userId, Player.class, true, false);
    }

    private User addUser(
            long userId, String username, String password,
            HttpServletRequest httpRequest) throws StorageException {
        User playerUser = new User();
        playerUser.setName(username);
        playerUser.setLogin(username);
        playerUser.setEmail(username + GAME_EMAIL_DOMAIN);
        playerUser.setPassword(password);
        playerUser.setTemporary(true);
        playerUser.setDeviceLimit(1);
        playerUser.setUserLimit(0);
        playerUser.setId(storage.addObject(playerUser, new Request(new Columns.Exclude("id"))));
        storage.updateObject(playerUser, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", playerUser.getId())));
        actionLogger.create(httpRequest, userId, playerUser);
        return playerUser;
    }

    private Device addDevice(
            long userId, String username, HttpServletRequest httpRequest) throws StorageException {
        Device device = new Device();
        device.setName(username);
        device.setUniqueId(username);
        device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, device);
        return device;
    }

    private Player addPlayerObject(
            long userId, User playerUser, Device device,
            HttpServletRequest httpRequest) throws StorageException {
        Player player = new Player();
        player.setUserId(playerUser.getId());
        player.setDeviceId(device.getId());
        player.setActive(true);
        player.setCreatedAt(new Date());
        player.setId(storage.addObject(player, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, player);
        return player;
    }

    private void updateUserPassword(
            long userId, long playerUserId, String password,
            HttpServletRequest httpRequest) throws Exception {
        validatePassword(password);
        User user = new User();
        user.setId(playerUserId);
        user.setPassword(password);
        storage.updateObject(user, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", playerUserId)));
        cacheManager.invalidateObject(true, User.class, playerUserId, ObjectOperation.UPDATE);
        actionLogger.edit(httpRequest, userId, user);
    }

    private GameMember addGameMember(
            long userId, Game game, Player player, String displayName, String role,
            HttpServletRequest httpRequest) throws StorageException {
        GameMember member = new GameMember();
        member.setGameId(game.getId());
        member.setPlayerId(player.getId());
        member.setDisplayName(displayName);
        member.setRole(role);
        member.setStatus(GameMember.STATUS_ACTIVE);
        member.setId(storage.addObject(member, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, member);
        return member;
    }

    private Set<Long> getActivePlayerIds(long gameId) throws StorageException {
        var playerIds = new HashSet<Long>();
        var members = storage.getObjects(GameMember.class, new Request(
                new Columns.Include("playerId"), new Condition.And(
                        new Condition.Equals("gameId", gameId),
                        new Condition.Equals("status", GameMember.STATUS_ACTIVE))));
        for (GameMember member : members) {
            playerIds.add(member.getPlayerId());
        }
        return playerIds;
    }

    private GameMember getGameMember(long gameId, long memberId) throws StorageException {
        return storage.getObject(GameMember.class, new Request(
                new Columns.All(), new Condition.And(
                        new Condition.Equals("id", memberId),
                        new Condition.Equals("gameId", gameId))));
    }

    private Player getPlayer(long playerId) throws StorageException {
        return storage.getObject(Player.class, new Request(
                new Columns.All(), new Condition.Equals("id", playerId)));
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username is too long");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }
    }

    private void checkUsernameAvailable(String username) throws StorageException {
        String lowerUsername = username.toLowerCase(Locale.ROOT);
        if (storage.getObject(User.class, new Request(
                new Columns.Include("id"), new Condition.Or(
                        new Condition.Equals("LOWER(login)", lowerUsername),
                        new Condition.Equals("LOWER(email)", lowerUsername + GAME_EMAIL_DOMAIN)))) != null) {
            throw new IllegalArgumentException("Username is already used");
        }
        if (storage.getObject(Device.class, new Request(
                new Columns.Include("id"), new Condition.Equals("LOWER(uniqueId)", lowerUsername))) != null) {
            throw new IllegalArgumentException("Username is already used as device identifier");
        }
    }

    private WizardReusablePlayer toReusablePlayer(Player player) throws StorageException {
        WizardReusablePlayer view = new WizardReusablePlayer();
        view.setPlayerId(player.getId());
        view.setUserId(player.getUserId());
        view.setDeviceId(player.getDeviceId());
        view.setActive(player.getActive());

        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", player.getUserId())));
        if (user != null) {
            view.setUsername(user.getLogin());
            view.setUserName(user.getName());
        }

        Device device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", player.getDeviceId())));
        if (device != null) {
            view.setDeviceName(device.getName());
            view.setDeviceUniqueId(device.getUniqueId());
            view.setClientSetupLink(clientSetupService.buildSetupLink(device.getUniqueId()));
        }
        return view;
    }

}
