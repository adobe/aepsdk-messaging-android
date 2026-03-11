/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import androidx.annotation.VisibleForTesting

/**
 * Class to store a mapping between valid [ContentCardSchemaData] and unique activity id's.
 */
class ContentCardMapper private constructor() {
    private val contentCardSchemaDataMap: MutableMap<String, ContentCardSchemaData> = HashMap()
    private val inboxPropositionItemMap: MutableMap<String, PropositionItem> = HashMap()

    companion object {
        @JvmStatic
        val instance: ContentCardMapper by lazy { ContentCardMapper() }
    }

    /**
     * Returns the [ContentCardSchemaData] for the given activity id.
     *
     * @param activityId to retrieve the [ContentCardSchemaData] for
     * @return the [ContentCardSchemaData] for the given activity id, or null if not found
     */
    fun getContentCardSchemaData(activityId: String): ContentCardSchemaData? {
        if (activityId.isEmpty()) {
            return null
        }
        return contentCardSchemaDataMap[activityId]
    }

    /**
     * Stores the [ContentCardSchemaData] for the given activity id.
     *
     * @param contentCardSchemaData the [ContentCardSchemaData] to store
     */
    @JvmName("storeContentCardSchemaData")
    internal fun storeContentCardSchemaData(contentCardSchemaData: ContentCardSchemaData) {
        if (contentCardSchemaData.parent.propositionReference == null) {
            return
        }
        contentCardSchemaDataMap[contentCardSchemaData.parent.proposition.activityId] =
            contentCardSchemaData
    }

    /**
     * Removes the [ContentCardSchemaData] for the given activity id.
     *
     * @param activityId to remove the [ContentCardSchemaData] for
     */
    @JvmName("removeContentCardSchemaData")
    internal fun removeContentCardSchemaData(activityId: String) {
        contentCardSchemaDataMap.remove(activityId)
    }

    /**
     * Stores the inbox [PropositionItem] for later display tracking, keyed by the inbox activity ID.
     *
     * @param inboxId the inbox activity ID (from InboxTemplate.id)
     * @param propositionItem the inbox [PropositionItem] to store
     */
    @JvmName("storeInboxPropositionItem")
    internal fun storeInboxPropositionItem(inboxId: String, propositionItem: PropositionItem) {
        if (inboxId.isEmpty()) {
            return
        }
        inboxPropositionItemMap[inboxId] = propositionItem
    }

    /**
     * Returns the stored inbox [PropositionItem] for the given inbox ID, if available.
     *
     * @param inboxId the inbox activity ID (from InboxTemplate.id)
     * @return the inbox [PropositionItem], or null if not stored
     */
    internal fun getInboxPropositionItem(inboxId: String): PropositionItem? {
        if (inboxId.isEmpty()) {
            return null
        }
        return inboxPropositionItemMap[inboxId]
    }

    @VisibleForTesting
    internal fun clear() {
        contentCardSchemaDataMap.clear()
        inboxPropositionItemMap.clear()
    }
}
