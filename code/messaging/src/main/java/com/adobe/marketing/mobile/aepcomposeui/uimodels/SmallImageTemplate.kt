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

package com.adobe.marketing.mobile.aepcomposeui.uimodels

/**
 * Class representing a small image template, which implements the [AepUITemplate] interface.
 *
 * @param id The unique identifier for this template.
 * @param title The title text and display settings.
 * @param body The body text and display settings.
 * @param image The details of the image to be displayed.
 * @param actionUrl If provided, interacting with this card will result in the opening of the actionUrl.
 * @param buttons The details for the small image template buttons.
 * @param dismissBtn The details for the small image template dismiss button.
 * @param isRead Indicates whether this template has been read.
 */
data class SmallImageTemplate(
    override val id: String,
    val title: AepText,
    val body: AepText? = null,
    val image: AepImage? = null,
    val actionUrl: String? = null,
    val buttons: List<AepButton>? = null,
    val dismissBtn: AepIcon? = null,
    val isRead: Boolean = false
) : AepUITemplate {

    /**
     * Returns the type of this template, which is [AepUITemplateType.SMALL_IMAGE].
     *
     * @return A string representing the type of the template.
     */
    override fun getType() = AepUITemplateType.SMALL_IMAGE
}
