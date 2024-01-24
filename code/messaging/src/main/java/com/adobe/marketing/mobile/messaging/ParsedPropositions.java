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

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsedPropositions {
    private final static String SELF_TAG = "ParsedPropositions";
    // store tracking information for propositions loaded into rules engines
    final Map<String, PropositionInfo> propositionInfoToCache = new HashMap<>();

    // non-in-app propositions should be cached and not persisted
    Map<Surface, List<MessagingProposition>> propositionsToCache = new HashMap<>();

    // in-app propositions don't need to stay in cache, but must be persisted
    // also need to store tracking info for in-app propositions as `PropositionInfo`
    Map<Surface, List<MessagingProposition>> propositionsToPersist = new HashMap<>();

    // in-app and feed rules that need to be applied to their respective rules engines
    final Map<InboundType, Map<Surface, List<LaunchRule>>> surfaceRulesByInboundType = new HashMap<>();

    ParsedPropositions(final Map<Surface, List<MessagingProposition>> propositions, final List<Surface> requestedSurfaces, final ExtensionApi extensionApi) {
        for (final List<MessagingProposition> propositionList : propositions.values()) {
            for (final MessagingProposition proposition : propositionList) {
                final String scope = proposition.getScope();
                boolean found = false;
                for (final Surface surface : requestedSurfaces) {
                    if (surface.getUri().equals(scope)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Ignoring proposition where scope (%s) does not match one of the expected surfaces (%s).", scope, requestedSurfaces.toString());
                    continue;
                }

                final Surface surface = Surface.fromUriString(scope);
                for (final MessagingPropositionItem propositionItem : proposition.getItems()) {
                    final String content = propositionItem.getContent();
                    if (StringUtils.isNullOrEmpty(content)) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Ignoring Proposition with empty content.");
                        continue;
                    }

                    final List<LaunchRule> parsedRules = JSONRulesParser.parse(content, extensionApi);
                    // iam and feed items will be wrapped in a valid rules engine rule - code-based experiences are not
                    if (MessagingUtils.isNullOrEmpty(parsedRules)) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Proposition did not contain a rule, adding as a code-based experience.");
                        propositionsToCache = MessagingUtils.updatePropositionMapForSurface(surface, proposition, propositionsToCache);
                        continue;
                    }

                    final List<RuleConsequence> consequences = parsedRules.get(0).getConsequenceList();
                    if (MessagingUtils.isNullOrEmpty(consequences)) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with null or empty consequences.");
                        continue;
                    }
                    for (final RuleConsequence consequence : consequences) {
                        // store reporting data for this payload for later use
                        final String messageId = consequence.getId();
                        if (!StringUtils.isNullOrEmpty(messageId)) {
                            final PropositionInfo propositionInfo = PropositionInfo.createFromProposition(proposition);
                            if (propositionInfo == null) {
                                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with missing / invalid proposition info.");
                                continue;
                            }
                            propositionInfoToCache.put(messageId, PropositionInfo.createFromProposition(proposition));
                        }

                        InboundType inboundType;
                        final boolean isInAppConsequence = InternalMessagingUtils.isInApp(consequence);
                        if (isInAppConsequence) {
                            inboundType = InboundType.INAPP;
                            propositionsToPersist = MessagingUtils.updatePropositionMapForSurface(surface, proposition, propositionsToPersist);
                        } else {
                            final String inboundTypeString = DataReader.optString(consequence.getDetail(), MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
                            inboundType = InboundType.fromString(inboundTypeString);
                            if (!InternalMessagingUtils.isFeedItem(consequence)) {
                                propositionsToCache = MessagingUtils.updatePropositionMapForSurface(surface, proposition, propositionsToCache);
                            }
                        }

                        mergeRules(parsedRules, surface, inboundType);
                    }
                }
            }
        }
    }

    private void mergeRules(final List<LaunchRule> rules, final Surface surface, final InboundType inboundType) {
        // get rules we may already have for this inboundType
        Map<Surface, List<LaunchRule>> tempRulesByInboundType = surfaceRulesByInboundType.get(inboundType) != null ? surfaceRulesByInboundType.get(inboundType) : new HashMap<>();

        // combine rules with existing
        tempRulesByInboundType = InternalMessagingUtils.updateRuleMapForSurface(surface, rules, tempRulesByInboundType);

        // apply up to surfaceRulesByInboundType
        surfaceRulesByInboundType.put(inboundType, tempRulesByInboundType);
    }
}
