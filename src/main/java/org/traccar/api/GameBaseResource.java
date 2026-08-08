package org.traccar.api;

import org.traccar.helper.LogAction;
import org.traccar.game.GamePermissionService;
import org.traccar.model.Game;
import org.traccar.model.GameBaseModel;
import org.traccar.model.ObjectOperation;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class GameBaseResource<T extends GameBaseModel> extends BaseResource {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Inject
    private GamePermissionService gamePermissionService;

    @Context
    private HttpServletRequest request;

    private final Class<T> baseClass;
    private final String orderField;

    protected GameBaseResource(Class<T> baseClass, String orderField) {
        this.baseClass = baseClass;
        this.orderField = orderField;
    }

    @GET
    public Stream<T> get() throws StorageException {
        return getVisibleGameObjects();
    }

    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        T entity = getGameObject(id);

        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        checkGame(entity.getGameId());

        return Response.ok(entity).build();
    }

    @POST
    public Response add(T entity) throws Exception {
        checkGameEdit(entity.getGameId());

        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));

        actionLogger.create(request, getUserId(), entity);

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(T entity) throws Exception {
        T existing = getGameObject(entity.getId());

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        checkGameEdit(existing.getGameId());

        entity.setGameId(existing.getGameId());
        storage.updateObject(
            entity,
            new Request(
                new Columns.Exclude("id", "gameId"),
                new Condition.Equals("id", entity.getId())));

        cacheManager.invalidateObject(true, baseClass, entity.getId(), ObjectOperation.UPDATE);
        actionLogger.edit(request, getUserId(), entity);

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        T existing = getGameObject(id);

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        checkGameEdit(existing.getGameId());

        storage.removeObject(baseClass, new Request(new Condition.Equals("id", id)));
        cacheManager.invalidateObject(true, baseClass, id, ObjectOperation.DELETE);

        actionLogger.remove(request, getUserId(), baseClass, id);

        return Response.noContent().build();
    }

    protected T getGameObject(long objectId) throws StorageException {
        return storage.getObject(baseClass, new Request(new Columns.All(), new Condition.Equals("id", objectId)));
    }

    protected void checkGame(long gameId) throws StorageException {
        gamePermissionService.checkPermission(getUserId(), gameId);
    }

    protected void checkGameEdit(long gameId) throws StorageException {
        gamePermissionService.checkEdit(getUserId(), gameId);
    }

    protected Stream<T> getVisibleGameObjects() throws StorageException {
        if (permissionsService.notAdmin(getUserId())) {
            var objects = new ArrayList<T>();
            var games = storage.getObjects(Game.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(User.class, getUserId(), Game.class), new Order("id")));
            for (Game game : games) {
                objects.addAll(storage.getObjects(baseClass, new Request(
                    new Columns.All(), new Condition.Equals("gameId", game.getId()), new Order(orderField))));
            }
            return objects.stream();
        }

        return storage.getObjectsStream(baseClass, new Request(new Columns.All(), null, new Order(orderField)));
    }
}
