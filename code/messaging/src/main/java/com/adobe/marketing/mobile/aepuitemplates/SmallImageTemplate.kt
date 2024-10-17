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

import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepDismissButton
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepImage
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText
import com.adobe.marketing.mobile.aepuitemplates.utils.AepUITemplateType

/**
 * Class representing a small image template, which implements the [AepUITemplate] interface.
 *
 * This class contains properties for an image URL, title, and description.
 */
class SmallImageTemplate : AepUITemplate {

    // TODO complete the implementation of this class when DataProvider is implemented

    /** The image component for the small image template, represented in model AEPImage. */
    var image: AepImage? = null

    /** The title for the small image template, represented in model AEPText. */
    var title: AepText? = null

    /** The description for the small image template, represented in model AEPText. */
    var description: AepText? = null

    /** The dismiss button for the small image template, represented in model AepDismissButton.
     * This is optional and can be null. */
    var dismissButton: AepDismissButton? = null

    /**
     * Returns the type of this template, which is [AepUITemplateType.SMALL_IMAGE].
     *
     * @return A string representing the type of the template.
     */
    override fun getType() = AepUITemplateType.SMALL_IMAGE
}
