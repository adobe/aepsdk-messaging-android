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

package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.Serializable;
import java.util.Map;

class PropositionInfo implements Serializable {
    final String id;
    final String scope;
    final Map<String, Object> scopeDetails;
    final String correlationId;
    final String activityId;

    private PropositionInfo(final Map<String, Object> propositionInfoMap) throws Exception {
        id = DataReader.getString(propositionInfoMap, MessagingConstants.PayloadKeys.ID);
        scope =
                DataReader.getString(
                        propositionInfoMap,
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE);
        scopeDetails =
                DataReader.getTypedMap(
                        Object.class,
                        propositionInfoMap,
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS);
        correlationId =
                DataReader.getString(scopeDetails, MessagingConstants.PayloadKeys.CORRELATION_ID);
        final Map<String, Object> activityMap =
                DataReader.optTypedMap(
                        Object.class, scopeDetails, MessagingConstants.PayloadKeys.ACTIVITY, null);
        if (MapUtils.isNullOrEmpty(activityMap)) {
            activityId = "";
        } else {
            activityId = DataReader.optString(activityMap, MessagingConstants.PayloadKeys.ID, "");
        }
    }

    private PropositionInfo(
            final String id, final String scope, final Map<String, Object> scopeDetails) {
        this.id = id;
        this.scope = scope;
        this.scopeDetails = scopeDetails;
        correlationId =
                DataReader.optString(
                        scopeDetails, MessagingConstants.PayloadKeys.CORRELATION_ID, "");
        final Map<String, Object> activityMap =
                DataReader.optTypedMap(
                        Object.class, scopeDetails, MessagingConstants.PayloadKeys.ACTIVITY, null);
        if (MapUtils.isNullOrEmpty(activityMap)) {
            activityId = "";
        } else {
            activityId = DataReader.optString(activityMap, MessagingConstants.PayloadKeys.ID, "");
        }
    }

    static PropositionInfo create(final Map<String, Object> propositionInfoMap) throws Exception {
        if (StringUtils.isNullOrEmpty(
                DataReader.getString(propositionInfoMap, MessagingConstants.PayloadKeys.ID))) {
            return null;
        }
        if (StringUtils.isNullOrEmpty(
                DataReader.getString(
                        propositionInfoMap,
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE))) {
            return null;
        }
        if (MapUtils.isNullOrEmpty(
                DataReader.getTypedMap(
                        Object.class,
                        propositionInfoMap,
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS))) {
            return null;
        }
        return new PropositionInfo(propositionInfoMap);
    }

    static PropositionInfo createFromProposition(final Proposition proposition) {
        if (proposition == null) {
            return null;
        }
        return new PropositionInfo(
                proposition.getUniqueId(), proposition.getScope(), proposition.getScopeDetails());
    }
}
