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

package com.adobe.marketing.mobile;

import java.util.Map;

/**
 * {@code MessagingRuleConsequence} class represents a Messaging rule consequence.
 */
class MessagingRuleConsequence {
    private final String id;
    private final String type;
    private final Map<String, Variant> detail;

    /**
     * Constructor
     *
     * @param id {@link String} containing unique consequence Id
     * @param type {@code String} containing message consequence type
     * @param detail {@code Map<String, Variant>} containing consequence detail
     */
    MessagingRuleConsequence(final String id, final String type,
                            final Map<String, Variant> detail) {
        this.id = id;
        this.type = type;
        this.detail = detail;
    }

    /**
     * Get the {@code id} for this {@code CampaignRuleConsequence} instance.
     *
     * @return {@link String} containing this {@link MessagingRuleConsequence#id}
     */
    String getId() {
        return id;
    }

    /**
     * Get the {@code type} for this {@code CampaignRuleConsequence} instance.
     *
     * @return {@link String} containing this {@link MessagingRuleConsequence#type}
     */
    String getType() {
        return type;
    }

    /**
     * Get the {@code detail} Map for this {@code CampaignRuleConsequence} instance.
     *
     * @return {@code Map<String,Variant>} containing this {@link MessagingRuleConsequence#detail}
     */
    Map<String, Variant> getDetail() {
        return detail;
    }
}