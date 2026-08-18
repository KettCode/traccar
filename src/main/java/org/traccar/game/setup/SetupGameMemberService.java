package org.traccar.game.setup;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.game.GamePermissionService;
import org.traccar.game.GameService;
import org.traccar.game.GameValidatorService;
import org.traccar.game.notification.GameNotificationService;
import org.traccar.game.notification.message.GameNotificationMessage;
import org.traccar.game.setup.request.SetupMemberRequest;
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
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

public class SetupGameMemberService {

    private static final int MAX_USER_EMAIL_LENGTH = 128;
    private static final String GENERATED_EMAIL_DOMAIN = "@game.local";
    private static final int MAX_TECHNICAL_IDENTIFIER_LENGTH = MAX_USER_EMAIL_LENGTH - GENERATED_EMAIL_DOMAIN.length();
    private static final Pattern SETUP_USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

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
    private GameNotificationService notificationService;

    @Inject
    private SetupStorage setupStorage;

    public boolean addMembers(
            long userId, long gameId, List<SetupMemberRequest> requests,
            HttpServletRequest httpRequest) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Game members are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        var addedMembers = new ArrayList<GameMember>();
        for (SetupMemberRequest request : requests) {
            addedMembers.add(addMember(userId, game, request, httpRequest));
        }
        notifyMembersChanged(gameId, addedMembers, GameNotificationMessage.TYPE_MEMBER_ADDED, true);
        return true;
    }

    public boolean addExistingPlayers(
            long userId, long gameId, List<GameMember> requests,
            HttpServletRequest httpRequest) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Existing players are required");
        }

        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        var addedMembers = new ArrayList<GameMember>();
        for (GameMember request : requests) {
            addedMembers.add(addExistingPlayer(userId, game, request, httpRequest));
        }
        notifyMembersChanged(gameId, addedMembers, GameNotificationMessage.TYPE_MEMBER_ADDED, true);
        return true;
    }

    public boolean updateMember(
            long userId, long gameId, long memberId, GameMember request,
            HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }
        if (request == null) {
            throw new IllegalArgumentException("Game player is required");
        }

        GameMember member = setupStorage.getGameMember(gameId, memberId);
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

        notifyMembersChanged(gameId, List.of(member), GameNotificationMessage.TYPE_MEMBER_CHANGED, true);

        return true;
    }

    public boolean removeMember(
            long userId, long gameId, long memberId, HttpServletRequest httpRequest) throws Exception {
        Game game = gameService.getEditableDraftGame(userId, gameId);
        if (game == null) {
            return false;
        }

        GameMember member = setupStorage.getGameMember(gameId, memberId);
        if (member == null) {
            return false;
        }

        storage.removeObject(GameMember.class, new Request(new Condition.Equals("id", memberId)));
        cacheManager.invalidateObject(true, GameMember.class, memberId, ObjectOperation.DELETE);
        actionLogger.remove(httpRequest, userId, GameMember.class, memberId);

        notificationService.notifyMembers(List.of(member), notificationService.createCurrentGameChangedMessage(
                gameId, GameNotificationMessage.TYPE_MEMBER_REMOVED));

        return true;
    }

    public void copyActiveMembers(
            long userId, long sourceGameId, Game targetGame,
            HttpServletRequest httpRequest) throws Exception {
        gameService.getEditableDraftGame(userId, targetGame.getId());

        var members = setupStorage.getActiveMembers(sourceGameId);
        var addedMembers = new ArrayList<GameMember>();
        for (GameMember member : members) {
            Player player = setupStorage.getPlayer(member.getPlayerId());
            if (player == null || !player.getActive() || player.getUserId() == 0 || player.getDeviceId() == 0) {
                continue;
            }
            validator.validateRole(member.getRole());
            ensurePlayerPermissions(userId, player, httpRequest);
            addedMembers.add(addGameMember(userId, targetGame, player, member.getDisplayName(), member.getRole(),
                    httpRequest));
        }
        if (!addedMembers.isEmpty()) {
            notifyMembersChanged(targetGame.getId(), addedMembers, GameNotificationMessage.TYPE_MEMBER_ADDED, true);
        }
    }

    private void notifyMembersChanged(
            long gameId, List<GameMember> members, String type, boolean stateRefresh) throws StorageException {
        notificationService.notifyMembers(members, notificationService.createCurrentGameChangedMessage(
                gameId, type, stateRefresh));
    }

    private GameMember addMember(
            long userId, Game game, SetupMemberRequest request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Game member is required");
        }

        String displayName = request.getDisplayName() != null ? request.getDisplayName().trim() : null;
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }
        String username = request.getUsername() != null ? request.getUsername().trim() : null;
        validateSetupUsername(username);
        String technicalUsername = getTechnicalUsername(username, game.getId());
        validateTechnicalUsername(technicalUsername);
        checkUsernameAvailable(technicalUsername);

        String role = request.getRole() != null ? request.getRole().trim() : null;
        validator.validateRole(role);
        validatePassword(request.getPassword());

        checkAddAccess(userId);

        User playerUser = addUser(userId, technicalUsername, request.getPassword(), httpRequest);
        Device device = addDevice(userId, technicalUsername, httpRequest);
        Player player = addPlayer(userId, displayName, playerUser, device, httpRequest);

        ensurePlayerPermissions(userId, player, httpRequest);

        return addGameMember(userId, game, player, displayName, role, httpRequest);
    }

    private GameMember addExistingPlayer(
            long userId, Game game, GameMember request,
            HttpServletRequest httpRequest) throws Exception {
        if (request == null || request.getPlayerId() == 0) {
            throw new IllegalArgumentException("Player is required");
        }

        permissionsService.checkPermission(Player.class, userId, request.getPlayerId());

        Player player = setupStorage.getPlayer(request.getPlayerId());
        if (player == null || !player.getActive() || player.getUserId() == 0 || player.getDeviceId() == 0) {
            throw new IllegalArgumentException("Player is not available");
        }
        if (setupStorage.getMemberPlayerIds(game.getId()).contains(player.getId())) {
            throw new IllegalArgumentException("Player is already in this game");
        }

        String role = request.getRole() != null ? request.getRole().trim() : null;
        validator.validateRole(role);

        String displayName = request.getDisplayName() != null ? request.getDisplayName().trim() : null;
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }

        ensurePlayerPermissions(userId, player, httpRequest);
        return addGameMember(userId, game, player, displayName, role, httpRequest);
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
            long userId, String technicalUsername, String password,
            HttpServletRequest httpRequest) throws StorageException {
        User playerUser = new User();
        playerUser.setName(technicalUsername);
        playerUser.setLogin(technicalUsername);
        playerUser.setEmail(technicalUsername + GENERATED_EMAIL_DOMAIN);
        playerUser.setPassword(password);
        playerUser.setReadonly(true);
        playerUser.setDeviceReadonly(true);
        playerUser.setDisableReports(true);
        playerUser.setDeviceLimit(0);
        playerUser.setUserLimit(0);
        playerUser.setId(storage.addObject(playerUser, new Request(new Columns.Exclude("id"))));
        storage.updateObject(playerUser, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", playerUser.getId())));
        actionLogger.create(httpRequest, userId, playerUser);
        return playerUser;
    }

    private Device addDevice(
            long userId, String technicalUsername, HttpServletRequest httpRequest) throws StorageException {
        Device device = new Device();
        device.setName(technicalUsername);
        device.setUniqueId(technicalUsername);
        device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, device);
        return device;
    }

    private Player addPlayer(
            long userId, String displayName, User playerUser, Device device,
            HttpServletRequest httpRequest) throws StorageException {
        Player player = new Player();
        player.setName(displayName);
        player.setUserId(playerUser.getId());
        player.setDeviceId(device.getId());
        player.setActive(true);
        player.setCreatedAt(new Date());
        player.setId(storage.addObject(player, new Request(new Columns.Exclude("id"))));
        actionLogger.create(httpRequest, userId, player);
        return player;
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

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private void validateSetupUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!SETUP_USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }
    }

    private void validateTechnicalUsername(String username) {
        if (username.length() > MAX_TECHNICAL_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("Username is too long");
        }
    }

    private String getTechnicalUsername(String username, long gameId) {
        return username.toLowerCase(Locale.ROOT) + "_" + gameId;
    }

    private void checkUsernameAvailable(String username) throws StorageException {
        String lowerUsername = username.toLowerCase(Locale.ROOT);
        if (storage.getObject(User.class, new Request(
                new Columns.Include("id"), new Condition.Or(
                        new Condition.Equals("LOWER(login)", lowerUsername),
                        new Condition.Equals("LOWER(email)", lowerUsername + GENERATED_EMAIL_DOMAIN)))) != null) {
            throw new IllegalArgumentException("Username is already used");
        }
        if (storage.getObject(Device.class, new Request(
                new Columns.Include("id"), new Condition.Equals("LOWER(uniqueId)", lowerUsername))) != null) {
            throw new IllegalArgumentException("Username is already used as device identifier");
        }
    }

}
