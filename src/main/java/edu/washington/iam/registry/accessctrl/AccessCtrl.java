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

import edu.washington.iam.registry.exception.AccessCtrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

public class AccessCtrl implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Boolean is2FASet;
    private Boolean auto2FA;
    private String groupAuto2FA;
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




    public AccessCtrl(){

        //is2FASet keeps track if some idiot (like your author) tries to enable conditional AND auto 2fa
        is2FASet = false;
        auto2FA = false;
        groupAuto2FA = "";
        conditional = false;
        conditionalGroup = "";
        entityId = "";
        uuid = null;
        startTime = null;
        endTime = null;
        updatedBy = "";

    }

    //2017-11-13 mattjm constructor taking XML document as argument removed (and deleted XMLProxyManager)

    //"conditional 2fa" is a virtual state--it means auto2fa is true and a group is set to a non-empty string
    public Boolean getCond2FA() {
        if (auto2FA && getGroupAuto2FA() != "")
        {
            return true;
        } else return false;
    }
    //for setting the virtual state above
    public void setCond2FA(String group) throws AccessCtrlException {
        if (is2FASet) { throw new AccessCtrlException("Can't sent Auto 2FA AND Conditional 2FA!!"); }
        if (StringUtils.isNotBlank(group))
        {
            this.auto2FA = true;
            this.groupAuto2FA = group;
            is2FASet = true; //2fa is set.  We can't set it again

        } else {
            throw new AccessCtrlException("tried to set conditional 2FA but provided empty or whitespace string for group name");
        }

    }

    //this is for external stuff to call--if this returns true then the entity ID uses "auto 2fa".  Handles the logic
    //of figuring out the auto2fa/group-is-populated permutations.
    public Boolean getAuto2FA() {
        if (auto2FA && getGroupAuto2FA() == "")
        {
            return true;
        } else return false;
    }
    //like the get methods, sets the state to "auto 2fa" without you having to figure out if the group field is populated
    //or not
    public void setAuto2FA(Boolean auto2FA) throws AccessCtrlException {
        if (is2FASet) { throw new AccessCtrlException("Can't sent Auto 2FA AND Conditional 2FA!!"); }
        this.auto2FA = auto2FA;
        this.groupAuto2FA = "";
        is2FASet = true; //2fa is set.  We can't set it again
    }

    //only use when DB is setting this property (doesn't have the safety features of the other methods)
    //DB needs to be able to set auto2fa and groupAuto2FA properties independently
    public void setAuto2FAInternal(Boolean auto2FA)
    {
        this.auto2FA = auto2FA;
    }
    //also for DB use only--DB needs to be able to get the "naked" auto2fa state
    public Boolean getAuto2FAInternal() {
        return this.auto2FA;
    }

    public String getGroupAuto2FA() {
        //if not null, empty, or only whitespace, return the string
        if (StringUtils.isNotBlank(groupAuto2FA)) { return groupAuto2FA; }
        //otherwise return a string that is definitely empty (avoids whitespace issues)
        else { return ""; }
    }

    public void setGroupAuto2FA(String groupAuto2FA) {
        this.groupAuto2FA = groupAuto2FA;
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

