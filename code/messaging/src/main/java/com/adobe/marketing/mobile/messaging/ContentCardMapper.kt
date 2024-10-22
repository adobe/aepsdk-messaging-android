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

import com.adobe.marketing.mobile.util.StringUtils

/**
 * Class to store a mapping between valid [ContentCardSchemaData] and unique proposition id's.
 */
class ContentCardMapper private constructor() {
    companion object {
        private val contentCardSchemaDataMap: MutableMap<String, ContentCardSchemaData> = HashMap()

        @Volatile
        private var instance: ContentCardMapper? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ContentCardMapper().also { instance = it }
            }
    }

    fun storeContentCardSchemaData(contentCardSchemaData: ContentCardSchemaData) {
        if (contentCardSchemaData.parent.propositionReference == null) {
            return
        }
        contentCardSchemaDataMap[contentCardSchemaData.parent.proposition.uniqueId] = contentCardSchemaData
    }

    fun removeContentCardSchemaDataFromMap(propositionId: String) {
        contentCardSchemaDataMap.remove(propositionId)
    }

    fun getContentCardSchemaDataForPropositionId(propositionId: String): ContentCardSchemaData? {
        if (StringUtils.isNullOrEmpty(propositionId)) {
            return null
        }
        return contentCardSchemaDataMap[propositionId]
    }
}
