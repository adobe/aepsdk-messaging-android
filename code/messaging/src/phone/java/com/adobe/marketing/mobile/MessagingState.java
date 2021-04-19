/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstant.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID;
import static com.adobe.marketing.mobile.MessagingConstant.SharedState.Configuration.GLOBAL_PRIVACY_STATUS;
import static com.adobe.marketing.mobile.MessagingConstant.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.MessagingConstant.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.MessagingConstant.SharedState.EdgeIdentity.IDENTITY_MAP;

/**
 * MessagingState is used to store configuration and identity information
 */
final class MessagingState {
    //Identity properties.
    private String ecid;

    // Messaging properties
    private String experienceEventDatasetId;


    void setState(final EventData configState, final EventData edgeIdentityState) {
        setConfigState(configState);
        setEdgeIdentityState(edgeIdentityState);
    }

    private void setConfigState(final EventData configState) {
        if (configState != null) {
            this.experienceEventDatasetId = configState.optString(EXPERIENCE_EVENT_DATASET_ID, "");
        }
    }

    private void setEdgeIdentityState(final EventData edgeIdentityState) {
        if (edgeIdentityState != null) {
            Map<String, Variant> variantMap = edgeIdentityState.optVariantMap(IDENTITY_MAP, null);
            if (variantMap != null && variantMap.get(ECID) != null) {
                List<Variant> ecids = variantMap.get(ECID).optVariantList(null);
                if (ecids != null && !ecids.isEmpty()) {
                    Map<String, Variant> ecidObject = ecids.get(0).optVariantMap(null);
                    if (ecidObject != null && ecidObject.containsKey(ID)) {
                        ecid = ecidObject.get(ID).optString(null);
                    }
                }
            }
        }
    }

    String getEcid() {
        return ecid;
    }

    String getExperienceEventDatasetId() {
        return experienceEventDatasetId;
    }
}
