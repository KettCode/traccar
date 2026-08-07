package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.Player;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.stream.Stream;

@Path("players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayerResource extends BaseObjectResource<Player> {

    public PlayerResource() {
        super(Player.class);
    }

    @GET
    public Stream<Player> get() throws StorageException {
        if (permissionsService.notAdmin(getUserId())) {
            return storage.getObjectsStream(baseClass, new Request(
                new Columns.All(), new Condition.Permission(User.class, getUserId(), baseClass), new Order("id")));
        }

        return storage.getObjectsStream(baseClass, new Request(new Columns.All(), null, new Order("id")));
    }

}
