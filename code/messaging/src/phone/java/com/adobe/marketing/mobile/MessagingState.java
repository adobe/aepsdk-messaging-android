/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID;
import static com.adobe.marketing.mobile.MessagingConstants.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.MessagingConstants.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP;

import java.util.List;
import java.util.Map;

/**
 * MessagingState is used to store configuration and identity information
 */
final class MessagingState {
    private final String SELF_TAG = "MessagingState";
    //Identity properties.
    private String ecid;

    // Messaging properties
    private String experienceEventDatasetId;

    void setState(final Map<String, Object> configState, final Map<String, Object> edgeIdentityState) {
        setConfigState(configState);
        setEdgeIdentityState(edgeIdentityState);
    }

    private void setConfigState(final Map<String, Object> configState) {
        if (configState != null) {
            Object expEventDatasetId = configState.get(EXPERIENCE_EVENT_DATASET_ID);
            if (expEventDatasetId instanceof String) {
                this.experienceEventDatasetId = (String) expEventDatasetId;
            }
        }
    }

    private void setEdgeIdentityState(final Map<String, Object> edgeIdentityState) {
        if (edgeIdentityState != null) {
            try {
                Object identityMapObj = edgeIdentityState.get(IDENTITY_MAP);
                if (identityMapObj instanceof Map) {
                    Map<String, Object> identityMap = (Map<String, Object>) identityMapObj;
                    Object ecidsObj = identityMap.get(ECID);
                    if (ecidsObj instanceof List) {
                        List<Object> ecids = (List<Object>) identityMap.get(ECID);
                        if (!ecids.isEmpty()) {
                            Object ecidObject = ecids.get(0);
                            if (ecidObject instanceof Map) {
                                Map<String, Object> ecid = (Map<String, Object>) ecids.get(0);
                                Object idObj = ecid.get(ID);
                                if (idObj instanceof String) {
                                    this.ecid = (String) idObj;
                                }
                            }
                        }
                    }
                }
            } catch (ClassCastException e) {
                Log.debug(MessagingConstants.LOG_TAG, "%s - Exception while trying to get the ecid. Error -> %s", SELF_TAG, e.getMessage());
            }
        }
    }

    String getEcid() {
        return ecid;
    }

    String getExperienceEventDatasetId() {
        return experienceEventDatasetId;
    }

    boolean isReadyForEvents() {
        return isConfigStateSet() && !StringUtils.isNullOrEmpty(ecid);
    }

    boolean isConfigStateSet() {
        return !StringUtils.isNullOrEmpty(experienceEventDatasetId);
    }
}
