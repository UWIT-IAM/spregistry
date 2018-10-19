/* ========================================================================
 * Copyright (c) 2012-2013 The University of Washington
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
 * ========================================================================
 */


package edu.washington.iam.registry.accessctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.UUID;

public class AccessCtrl implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());



    private Boolean auto2FA;
    private Boolean conditional;
    private String conditionalGroup;
    private String entityId;
    private UUID uuid;
    private String startTime;
    private String endTime;
    private String updatedBy;


    private String safePy(String in) {
       return in.replaceAll("\"","\\\"");
    }

    public AccessCtrl(){}

    //2017-11-13 mattjm constructor taking XML document as argument removed (and deleted XMLProxyManager)


    public Boolean getAuto2FA() {
        return auto2FA;
    }

    public void setAuto2FA(Boolean auto2FA) {
        this.auto2FA = auto2FA;
    }

    public Boolean getConditional() {
        return conditional;
    }

    public void setConditional(Boolean conditional) {
        this.conditional = conditional;
    }

    public String getConditionalGroup() {
        return conditionalGroup;
    }

    public void setConditionalGroup(String conditionalGroup) {
        this.conditionalGroup = conditionalGroup;
    }
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

