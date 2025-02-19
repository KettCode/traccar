/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.traccar.api.SimpleObjectResource;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.helper.LogAction;
import org.traccar.model.Group;

import jakarta.ws.rs.core.MediaType;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

@Path("groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupResource extends SimpleObjectResource<Group> {

    @Inject
    private ConnectionManager connectionManager;

    public GroupResource() {
        super(Group.class, "name");
    }

    @POST
    public Response add(Group entity) throws Exception {
        var response = super.add(entity);
        if(entity.getManhuntRole() == 2)
            connectionManager.scheduleUpdates(entity);
        return response;
    }

    @Path("{id}")
    @PUT
    public Response update(Group entity) throws Exception {
        var response = super.update(entity);
        if(entity.getManhuntRole() == 2)
            connectionManager.scheduleUpdates(entity);
        else
            connectionManager.cancelScheduler(entity.getId());
        return response;
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        var response = super.remove(id);
        connectionManager.cancelScheduler(id);
        return response;
    }

}
