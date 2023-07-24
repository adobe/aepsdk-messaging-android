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

package com.adobe.marketing.mobile;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * A {@link Proposition} object encapsulates offers and the information needed for tracking offer interactions.
 */
public class Proposition {
    // Unique proposition identifier
    private final String uniqueId;
    // Scope string
    private final String scope;
    // Scope details map
    private final Map<String, Object> scopeDetails;
    // List containing proposition decision items
    private final List<PropositionItem> propositionItems;

    public Proposition(final String uniqueId, final String scope, final Map<String, Object> scopeDetails, final List<PropositionItem> propositionItems) {
        this.uniqueId = uniqueId;
        this.scope = scope;
        this.scopeDetails = scopeDetails;
        this.propositionItems = propositionItems;
        for (final PropositionItem item: this.propositionItems) {
            if (item.getProposition() == null) {
                item.proposition = new WeakReference<>(this);
            }
        }
    }

    /**
     * Gets the {@code Proposition} identifier.
     *
     * @return {@link String} containing the {@link Proposition} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code Proposition} items.
     *
     * @return {@code List<PropositionItem>} containing the {@link Proposition} items.
     */
    public List<PropositionItem> getItems() {
        return propositionItems;
    }

    /**
     * Gets the {@code Proposition} scope.
     *
     * @return {@link String} containing the encoded {@link Proposition} scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the {@code Proposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link Proposition} scope details.
     */
    public Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }
}