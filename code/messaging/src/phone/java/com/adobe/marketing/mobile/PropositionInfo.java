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

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.CORRELATION_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.SCOPE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.SCOPE_DETAILS;

import java.util.Map;

class PropositionInfo {
    final private String id;
    final private String scope;
    final private Map<String, Object> scopeDetails;

    PropositionInfo(final Map<String, Object> propositionInfoMap) {
        this.id = (String) propositionInfoMap.get(ID);
        this.scope = (String) propositionInfoMap.get(SCOPE);
        this.scopeDetails = (Map<String, Object>) propositionInfoMap.get(SCOPE_DETAILS);
    }

    String getId() {
        return id;
    }

    String getScope() {
        return scope;
    }

    Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    String getCorrelationId() {
        return (String) scopeDetails.get(CORRELATION_ID);
    }
}
