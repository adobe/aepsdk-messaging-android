/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate

/**
 * Extension function to get the meta data for the given [AepUI].
 *
 * @return the meta data as a [MutableMap] or null if the [AepUI] does not have meta data.
 */

fun AepUI<*, *>.getMeta(): Map<String, Any>? {
    val id = when (val template = this.getTemplate()) {
        is SmallImageTemplate -> template.id
        is LargeImageTemplate -> template.id
        is ImageOnlyTemplate -> template.id
        else -> return null
    }
    return ContentCardMapper.instance.getContentCardSchemaData(id)?.getMeta()
}
