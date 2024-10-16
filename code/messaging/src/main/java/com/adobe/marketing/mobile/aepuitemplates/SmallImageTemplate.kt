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

package com.adobe.marketing.mobile.aepuitemplates

import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPButton
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPImage
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPText
import com.adobe.marketing.mobile.aepuitemplates.utils.AepUITemplateType
import com.adobe.marketing.mobile.messaging.UiTemplateConstructionFailedException
import org.json.JSONObject

/**
 * Class representing a small image template, which implements the [AepUITemplate] interface.
 *
 * This class contains properties for an image URL, title, and description.
 */
class SmallImageTemplate(content: String) : AepUITemplate {

    /** Title text and display settings  */
    internal val title: AEPText

    /** Body text and display settings */
    internal val body: AEPText?

    /** The details of the image to be displayed. */
    internal val image: AEPImage?

    /** If provided, interacting with this card will result in the opening of the actionUrl. */
    internal val actionUrl: String?

    /** The details of the buttons. */
    internal val buttons: List<AEPButton>?

    /** The description for the small image template. */
    internal val dismissBtn: JSONObject?

    init {
        try {
            val contentJson = JSONObject(content)
            title = AEPText(contentJson.getJSONObject("title"))
            body = AEPText(contentJson.optJSONObject("body"))
            image = AEPImage(contentJson.optJSONObject("image"))
            actionUrl = contentJson.optString("actionUrl", "")
            val buttonArray = contentJson.optJSONArray("buttons")
            buttons = buttonArray?.let {
                (0 until it.length()).mapNotNull { index ->
                    AEPButton(it.optJSONObject(index))
                }
            }
            dismissBtn = contentJson.optJSONObject("dismissBtn")
        } catch (e: Exception) {
            throw UiTemplateConstructionFailedException("Exception occurred while constructing SmallImageTemplate: ${e.localizedMessage}")
        }
    }

    /**
     * Returns the type of this template, which is [AepUITemplateType.SMALL_IMAGE].
     *
     * @return A string representing the type of the template.
     */
    override fun getType() = AepUITemplateType.SMALL_IMAGE
}
