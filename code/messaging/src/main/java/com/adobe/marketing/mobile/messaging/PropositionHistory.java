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

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.Map;

final class PropositionHistory {
    private PropositionHistory() {}

    private static final String SELF_TAG = "PropositionHistory";
    /**
     * Dispatches an event to be recorded in Event History. If `activityId` is an empty string,
     * calling this function results in a no-op
     *
     * @param activityId {@link String} the Activity ID of the proposition being recorded.
     * @param eventType {@link MessagingEdgeEventType} the type of event being recorded.
     * @param interaction {@code String} optional value containing the specific interaction
     *     recorded.
     */
    static void record(
            @NonNull final String activityId,
            @NonNull final MessagingEdgeEventType eventType,
            @Nullable String interaction) {
        if (StringUtils.isNullOrEmpty(activityId)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Ignoring request to record PropositionHistory - activityId is empty.");
            return;
        }

        // create map for event history
        final Map<String, String> iamHistoryMap = new HashMap<>();
        iamHistoryMap.put(EVENT_TYPE, eventType.getPropositionEventType());
        iamHistoryMap.put(MESSAGE_ID, activityId);
        iamHistoryMap.put(
                TRACKING_ACTION, (StringUtils.isNullOrEmpty(interaction) ? "" : interaction));

        // wrap history in an "iam" object
        final Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put(MessagingConstants.EventDataKeys.IAM_HISTORY, iamHistoryMap);

        // create the mask for storing event history
        final String[] mask = {
            MessagingConstants.EventMask.Mask.EVENT_TYPE,
            MessagingConstants.EventMask.Mask.MESSAGE_ID,
            MessagingConstants.EventMask.Mask.TRACKING_ACTION
        };

        final Event event =
                new Event.Builder(
                                MessagingConstants.EventName.EVENT_HISTORY_WRITE,
                                MessagingConstants.EventType.MESSAGING,
                                MessagingConstants.EventSource.EVENT_HISTORY_WRITE,
                                mask)
                        .setEventData(eventHistoryData)
                        .build();

        MobileCore.dispatchEvent(event);
    }
}
