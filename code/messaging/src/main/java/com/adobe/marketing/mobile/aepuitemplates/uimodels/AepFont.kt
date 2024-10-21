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
 * Data class representing font styling in the UI.
 *
 * @property name The name of the font.
 * @property size The size of the font.
 * @property weight The weight of the font (e.g., bold, regular).
 * @property style A list of styles for the font (e.g., italic, underline).
 **/
data class AepFont(
    val name: String? = null,
    val size: Int? = null,
    val weight: String? = null,
    val style: List<String>? = null
)
