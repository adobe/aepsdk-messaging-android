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

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsedPropositions {
    private final static String SELF_TAG = "ParsedPropositions";
    // store tracking information for propositions loaded into rules engines
    final Map<String, PropositionInfo> propositionInfoToCache = new HashMap<>();

    // non-in-app propositions should be cached and not persisted
    Map<Surface, List<Proposition>> propositionsToCache = new HashMap<>();

    // in-app propositions don't need to stay in cache, but must be persisted
    // also need to store tracking info for in-app propositions as `PropositionInfo`
    Map<Surface, List<Proposition>> propositionsToPersist = new HashMap<>();

    // in-app and feed rules that need to be applied to their respective rules engines
    final Map<SchemaType, Map<Surface, List<LaunchRule>>> surfaceRulesBySchemaType = new HashMap<>();

    ParsedPropositions(final Map<Surface, List<Proposition>> propositions, final List<Surface> requestedSurfaces, final ExtensionApi extensionApi) {
        for (final List<Proposition> propositionList : propositions.values()) {
            for (final Proposition proposition : propositionList) {
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

                if (MessagingUtils.isNullOrEmpty(proposition.getItems())) {
                    continue;
                }

                final Surface surface = Surface.fromUriString(scope);
                final PropositionItem firstPropositionItem = proposition.getItems().get(0);
                switch (firstPropositionItem.getSchema()) {
                    case RULESET :
                        final JSONObject content = new JSONObject(firstPropositionItem.getData());
                        final List<LaunchRule> parsedRules = JSONRulesParser.parse(content.toString(), extensionApi);
                        // iam and feed items will be wrapped in a valid rules engine rule - code-based experiences are not
                        if (MessagingUtils.isNullOrEmpty(parsedRules)) {
                            break;
                        }
                        final List<RuleConsequence> consequences = parsedRules.get(0).getConsequenceList();
                        if (MessagingUtils.isNullOrEmpty(consequences)) {
                            break;
                        }
                        final RuleConsequence consequence = consequences.get(0);
                        final PropositionItem schemaConsequence = PropositionItem.fromRuleConsequence(consequence);
                        if (schemaConsequence == null) {
                            break;
                        }
                        switch (schemaConsequence.getSchema()) {
                            case INAPP:
                            case DEFAULT_CONTENT:
                                final PropositionInfo propositionInfo = PropositionInfo.createFromProposition(proposition);
                                if (propositionInfo == null) {
                                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with missing / invalid proposition info.");
                                    break;
                                }
                                propositionInfoToCache.put(consequence.getId(),propositionInfo);
                                propositionsToPersist = MessagingUtils.updatePropositionMapForSurface(surface, proposition, propositionsToPersist);
                                mergeRules(parsedRules, surface, SchemaType.INAPP);
                                break;
                            case FEED:
                                final PropositionInfo feedPropositionInfo = PropositionInfo.createFromProposition(proposition);
                                if (feedPropositionInfo == null) {
                                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with missing / invalid proposition info.");
                                    break;
                                }
                                propositionInfoToCache.put(consequence.getId(), feedPropositionInfo);
                                mergeRules(parsedRules, surface, SchemaType.FEED);
                                break;
                            default:
                                break;
                        }
                        break;
                    case JSON_CONTENT:
                    case HTML_CONTENT:
                    case DEFAULT_CONTENT:
                        propositionsToCache = MessagingUtils.updatePropositionMapForSurface(surface, proposition, propositionsToCache);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void mergeRules(final List<LaunchRule> rules, final Surface surface, final SchemaType schemaType) {
        // get rules we may already have for this inboundType
        Map<Surface, List<LaunchRule>> tempRulesByInboundType = surfaceRulesBySchemaType.get(schemaType) != null ? surfaceRulesBySchemaType.get(schemaType) : new HashMap<>();

        // combine rules with existing
        tempRulesByInboundType = InternalMessagingUtils.updateRuleMapForSurface(surface, rules, tempRulesByInboundType);

        // apply up to surfaceRulesByInboundType
        surfaceRulesBySchemaType.put(schemaType, tempRulesByInboundType);
    }
}
