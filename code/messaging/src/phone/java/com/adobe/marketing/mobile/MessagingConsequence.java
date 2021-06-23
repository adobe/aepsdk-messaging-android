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
 * {@code MessagingConsequence} class represents a Messaging extension rule consequence present in an Edge response event from Offers.
 */
class MessagingConsequence {
    private final String id;
    private final String type;
    private final Map<String, Variant> details;

    /**
     * Constructor
     *
     * @param id {@link String} containing unique consequence Id
     * @param type {@code String} containing message consequence type
     * @param details {@code Map<String, Variant>} containing consequence detail
     */
    MessagingConsequence(final String id, final String type,
                            final Map<String, Variant> details) {
        this.id = id;
        this.type = type;
        this.details = details;
    }

    /**
     * Get the {@code id} for this {@code MessagingConsequence} instance.
     *
     * @return {@link String} containing this {@link MessagingConsequence#id}
     */
    String getId() {
        return id;
    }

    /**
     * Get the {@code type} for this {@code MessagingRuleConsequence} instance.
     *
     * @return {@link String} containing this {@link MessagingConsequence#type}
     */
    String getType() {
        return type;
    }

    /**
     * Get the {@code detail} Map for this {@code MessagingRuleConsequence} instance.
     *
     * @return {@code Map<String,Variant>} containing this {@link MessagingConsequence#details}
     */
    Map<String, Variant> getDetails() {
        return details;
    }
}