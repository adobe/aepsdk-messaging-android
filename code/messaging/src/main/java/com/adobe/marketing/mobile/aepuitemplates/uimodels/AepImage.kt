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
 * Data class representing an image element in the UI.
 *
 * @property url The URL of the image.
 * @property darkUrl The URL of the image for dark mode.
 * @property bundle The resource bundle for the image.
 * @property darkBundle The resource bundle for the image in dark mode.
 * @property icon The icon name or identifier.
 * @property iconSize The size of the icon.
 * @property iconColor The color of the icon.
 * @property alt The alt text for the image.
 * @property placeholder The placeholder image for the image.
 */
data class AepImage(
    val url: String? = null,
    val darkUrl: String? = null,
    val bundle: String? = null,
    val darkBundle: String? = null,
    // TODO: verify Android equivalent of icon
    val icon: String? = null,
    val iconSize: Float? = null,
    val iconColor: AepColor? = null,
    val alt: String? = null,
    val placeholder: String? = null
)
