/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.MessageFeedKeys.SURFACE;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Inbound;
import com.adobe.marketing.mobile.Surface;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FeedRulesEngine extends LaunchRulesEngine {
    final ExtensionApi extensionApi;

    FeedRulesEngine(@NonNull String name, @NonNull ExtensionApi extensionApi) {
        super(name, extensionApi);
        this.extensionApi = extensionApi;
    }

    /**
     * Evaluates the supplied event against the all current rules and returns a {@link
     * Map<Surface, List<Inbound>>} created from the rules that matched the supplied event.
     *
     * @param event the event to be evaluated
     * @return a {@code Map<String ,List<Inbound>>} containing inbound content for the given event
     */
    Map<Surface, List<Inbound>> evaluate(@NonNull final Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot evaluate null event.");
        }

        final List<RuleConsequence> consequences = evaluateEvent(event);
        if (MessagingUtils.isNullOrEmpty(consequences)) {
            return null;
        }

        final Map<Surface, List<Inbound>> inboundMessages = new HashMap<>();
        for (final RuleConsequence consequence : consequences) {
            if (consequence == null) {
                continue;
            }

            final Map details = consequence.getDetail();
            final Inbound inboundMessage = Inbound.fromConsequenceDetails(details);
            if (inboundMessage == null) {
                continue;
            }

            final Map metadata = inboundMessage.getMeta();
            if (MapUtils.isNullOrEmpty(metadata)) {
                continue;
            }

            final Surface surface = Surface.fromUriString(DataReader.optString(metadata, SURFACE, ""));
            if (!surface.isValid()) {
                continue;
            }

            if (inboundMessages.get(surface) != null) {
                final List<Inbound> inboundMessageList = new ArrayList<>(inboundMessages.get(surface));
                inboundMessageList.add(inboundMessage);
                inboundMessages.put(surface, inboundMessageList);
            } else {
                inboundMessages.put(surface, Collections.singletonList(inboundMessage));
            }
        }
        return inboundMessages;
    }
}
