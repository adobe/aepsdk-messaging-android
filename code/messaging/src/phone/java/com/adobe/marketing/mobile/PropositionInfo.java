/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.PayloadKeys.CORRELATION_ID;
import static com.adobe.marketing.mobile.MessagingConstants.PayloadKeys.ID;
import static com.adobe.marketing.mobile.MessagingConstants.PayloadKeys.SCOPE;
import static com.adobe.marketing.mobile.MessagingConstants.PayloadKeys.SCOPE_DETAILS;
import static com.adobe.marketing.mobile.MessagingConstants.PayloadKeys.ACTIVITY;

import java.io.Serializable;
import java.util.Map;

class PropositionInfo implements Serializable {
    final String id;
    final String scope;
    final Map<String, Object> scopeDetails;
    final String correlationId;
    final String activityId;


    PropositionInfo(final Map<String, Object> propositionInfoMap) {
        id = (String) propositionInfoMap.get(ID);
        scope = (String) propositionInfoMap.get(SCOPE);
        scopeDetails = (Map<String, Object>) propositionInfoMap.get(SCOPE_DETAILS);
        if (!MessagingUtils.isMapNullOrEmpty(scopeDetails)) {
            correlationId = (String) scopeDetails.get(CORRELATION_ID);
            final Map<String, Object> activityMap = (Map<String, Object>) scopeDetails.get(ACTIVITY);
            activityId = MessagingUtils.isMapNullOrEmpty(activityMap) ? "" : (String) activityMap.get(ID);
        } else {
            correlationId = "";
            activityId = "";
        }
    }
}