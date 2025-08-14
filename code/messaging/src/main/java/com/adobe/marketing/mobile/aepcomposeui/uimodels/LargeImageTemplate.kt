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

package com.adobe.marketing.mobile.aepcomposeui.uimodels

/**
 * Class representing a large image template, which implements the [AepUITemplate] interface.
 *
 * @param id The unique identifier for this template.
 * @property title The title text and display settings.
 * @property body The body text and display settings.
 * @property image The details of the image to be displayed.
 * @property actionUrl If provided, interacting with this card will result in the opening of the actionUrl.
 * @property buttons The details for the large image template buttons.
 * @property dismissBtn The details for the large image template dismiss button.
 */
data class LargeImageTemplate(
    val id: String,
    val title: AepText,
    val body: AepText? = null,
    val image: AepImage? = null,
    val actionUrl: String? = null,
    val buttons: List<AepButton>? = null,
    val dismissBtn: AepIcon? = null
) : AepUITemplate {

    /**
     * Returns the type of this template, which is [AepUITemplateType.LARGE_IMAGE].
     *
     * @return A string representing the type of the template.
     */
    override fun getType() = AepUITemplateType.LARGE_IMAGE
}
