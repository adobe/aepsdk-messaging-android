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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class PropositionPayload implements Serializable {
    final PropositionInfo propositionInfo;
    final List<PayloadItem> items = new ArrayList<>();

    private PropositionPayload(final PropositionInfo propositionInfo, final List<Map<String, Object>> items) {
        this.propositionInfo = propositionInfo;
        final Iterator iterator = items.listIterator();
        while (iterator.hasNext()) {
            final PayloadItem payloadItem = new PayloadItem((Map<String, Object>) iterator.next());
            this.items.add(payloadItem);
        }
    }

    static PropositionPayload create(final PropositionInfo propositionInfo, final List<Map<String, Object>> items) {
        if (propositionInfo == null) {
            return null;
        }

        if (items == null || items.size() == 0) {
            return null;
        }

        return new PropositionPayload(propositionInfo, items);
    }
}
