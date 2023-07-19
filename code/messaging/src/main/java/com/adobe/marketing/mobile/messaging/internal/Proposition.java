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

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.ITEMS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.SCOPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.SCOPE_DETAILS;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link Proposition} object encapsulates offers and the information needed for tracking offer interactions.
 */
class Proposition {
    // Unique proposition identifier
    final String uniqueId;
    // Scope string
    final String scope;
    // Scope details map
    private final Map<String, Object> scopeDetails;
    // List containing proposition decision items
    private final List<PropositionItem> propositionItems;

    Proposition(final Map<String, Object> propositionInfoMap) throws Exception {
        uniqueId = DataReader.getString(propositionInfoMap, ID);
        scope = DataReader.getString(propositionInfoMap, SCOPE);
        if (StringUtils.isNullOrEmpty(uniqueId) || StringUtils.isNullOrEmpty(scope)) {
            throw new Exception("id and scope are required for constructing Proposition objects.");
        }
        scopeDetails = DataReader.getTypedMap(Object.class, propositionInfoMap, SCOPE_DETAILS);
        final List<Map<String, Object>> itemMap = DataReader.getTypedListOfMap(Object.class, propositionInfoMap, ITEMS);
        propositionItems = new ArrayList<>();
        for (int i = 0; i < itemMap.size(); i++) {
            propositionItems.add(new PropositionItem(itemMap.get(i)));
            propositionItems.get(i).proposition = new WeakReference<>(this);
        }
    }

    List<PropositionItem> getPropositionItems() {
        return propositionItems;
    }
}