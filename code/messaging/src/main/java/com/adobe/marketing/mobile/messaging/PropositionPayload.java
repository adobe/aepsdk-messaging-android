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

import com.adobe.marketing.mobile.services.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PropositionPayload implements Serializable {
    private final static String SELF_TAG = "PropositionPayload";
    final PropositionInfo propositionInfo;
    final List<PayloadItem> items = new ArrayList<>();

    private PropositionPayload(final PropositionInfo propositionInfo, final List<Map<String, Object>> items) {
        this.propositionInfo = propositionInfo;
        for (final Map<String, Object> item : items) {
            try {
                PayloadItem payloadItem = new PayloadItem(item);
                this.items.add(payloadItem);
            } catch (final Exception exception) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create a PayloadItem, an exception occurred: %s.", exception.getLocalizedMessage());
            }
        }
    }

    static PropositionPayload create(final PropositionInfo propositionInfo, final List<Map<String, Object>> items) {
        if (propositionInfo == null) {
            return null;
        }

        if (MessagingUtils.isNullOrEmpty(items) || items.size() == 0) {
            return null;
        }

        return new PropositionPayload(propositionInfo, items);
    }
}