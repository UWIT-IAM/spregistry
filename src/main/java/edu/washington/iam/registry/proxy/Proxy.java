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


package edu.washington.iam.registry.proxy;

import java.io.Serializable;

import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.ProxyException;
import java.util.UUID;

public class Proxy implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UUID uuid;
    private String entityId;
    private boolean socialActive;
    private String updatedBy;
    private String startTime;
    private String endTime;


    private String safePy(String in) {
       return in.replaceAll("\"","\\\"");
    }

    public Proxy (){}

    //2017-11-13 mattjm constructor taking XML document as argument removed (and deleted XMLProxyManager)


    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    public String getEntityId() {
        return (entityId);
    }

    public void setSocialActive(boolean socialActive) {
        this.socialActive = socialActive;
    }
    public boolean getSocialActive() { return (socialActive); }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public UUID getUuid() { return uuid; }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

}

