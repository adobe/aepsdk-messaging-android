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

import com.adobe.marketing.mobile.aepuitemplates.utils.AepUITemplateType

/**
 * Class representing a small image template, which implements the [AepUITemplate] interface.
 *
 * This class contains properties for an image URL, title, and description.
 */
class SmallImageTemplate : AepUITemplate {
    /** The URL of the image for the small image template. */
    val imageUrl: String = ""

    /** The title for the small image template. */
    val title: String = ""

    /** The description for the small image template. */
    val description: String = ""

    /**
     * Returns the type of this template, which is [AepUITemplateType.SMALL_IMAGE].
     *
     * @return A string representing the type of the template.
     */
    override fun getType() = AepUITemplateType.SMALL_IMAGE
}
