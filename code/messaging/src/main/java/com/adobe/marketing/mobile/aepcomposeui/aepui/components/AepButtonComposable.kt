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

package com.adobe.marketing.mobile.aepcomposeui.aepui.components

import android.graphics.Color.parseColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText

/**
 * A composable function that displays a button element with customizable properties.
 *
 * @param interactId A unique identifier for the button interaction. This is a mandatory field.
 * @param text An [AepText] object that contains the text to be displayed on the button.
 *             The text.content is mandatory for this button.
 * @param actionUrl A string representing the URL to be triggered when the button is clicked.
 *                  This is a mandatory field.
 * @param borWidth Optional. An integer representing the width of the button's border in dp.
 * @param borColor Optional. A string representing the color of the button's border in hex format (e.g., "#FFFFFF").
 *                 If not provided, the default border color is transparent.
 */
@Composable
internal fun AepButtonComposable(
    interactId: String,
    text: AepText,
    actionUrl: String,
    borWidth: Int? = null,
    borColor: String? = null,
    bgColor: String? = null
) {
    // TODO: Implement the AEPButtonComposable
    // Here code added as placeholder for reference, actual implementation is pending

    // Check for mandatory fields
    require(interactId.isNotEmpty()) { "interactId is mandatory for AEPButtonComposable" }
    require(text.content != null) { "text.content is mandatory for AEPButtonComposable" }
    require(actionUrl.isNotEmpty()) { "actionUrl is mandatory for AEPButtonComposable" }

    // Set optional border properties
    val borderColor = borColor?.let { Color(parseColor(it)) } ?: Color.Transparent
    val backgroundColor = bgColor?.let { Color(parseColor(it)) } ?: MaterialTheme.colors.primary
    var elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 8.dp)
    if (borWidth != null && borWidth > 0) {
        elevation = ButtonDefaults.elevation()
    }

    // Composable for Button
    Button(
        onClick = {
            // TODO handle button click
        },
        elevation = elevation,
        border = borWidth?.let { BorderStroke(it.dp, borderColor) },
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
        modifier = Modifier.padding(8.dp) // Modifier with padding to avoid layout issues
    ) {
        // Use AEPTextComposable to render the button text
        // TODO to complete
    }
}