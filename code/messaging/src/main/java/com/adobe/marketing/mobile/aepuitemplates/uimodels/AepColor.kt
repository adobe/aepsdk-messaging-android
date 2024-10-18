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

import kotlinx.serialization.Serializable

/**
 * Represents the colors used for UI elements in both light and dark themes.
 *
 * This data class holds the colors for each UI element, where colors are represented
 * as 8-digit hex strings that include an alpha value.
 *
 * @property lightColour The color for the light theme, represented as an 8-digit hex string
 * (including alpha). For example, `#FFDA94CC`.
 * @property darkColour The color for the dark theme, represented as an 8-digit hex string
 * (including alpha). It is optional and can be null if not provided. For example, `#FFDA94CC`.
 *
 */
@Serializable
data class AepColor(
    val lightColour: String,
    val darkColour: String? = null
)
