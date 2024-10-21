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

package com.adobe.marketing.mobile.aepuitemplates.uimodels

/**
 * Data class representing a button element in the UI.
 *
 * @property id The unique ID for the button.
 * @property actionUrl The URL to be opened when the button is clicked.
 * @property text The text to be displayed on the button, represented by an [AepText] object.
 * @property borderWidth The border width of the button.
 * @property borderColor The border color of the button, represented by an [AepColor] object.
 * @property backgroundColour The background color of the button, represented by an [AepColor] object.
 *
 */
data class AepButton(
    val id: String,
    val actionUrl: String,
    val text: AepText,
    val borderWidth: Float? = null,
    val borderColor: AepColor? = null,
    val backgroundColour: AepColor? = null
)
