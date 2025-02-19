/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_groups")
public class Group extends GroupedModel {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private long manhuntRole;
    public long getManhuntRole() { return manhuntRole; }
    public void setManhuntRole(long manhuntRole) { this.manhuntRole = manhuntRole; }

    private long frequency;
    public long getFrequency() {
        return frequency;
    }
    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    private long speedHunts;
    public long getSpeedHunts() { return speedHunts; }
    public void setSpeedHunts(long speedHunts) { this.speedHunts = speedHunts; }

    private long speedHuntRequests;
    public long getSpeedHuntRequests() { return speedHuntRequests; }
    public void setSpeedHuntRequests(long speedHuntRequests) { this.speedHuntRequests = speedHuntRequests; }
}
