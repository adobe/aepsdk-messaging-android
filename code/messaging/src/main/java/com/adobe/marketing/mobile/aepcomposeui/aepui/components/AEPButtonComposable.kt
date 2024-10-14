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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AEPText

/**
 * A composable function that displays a button element with customizable properties.
 *
 * @param interactId A unique identifier for the button interaction. This is a mandatory field.
 * @param text An [AEPText] object that contains the text to be displayed on the button.
 *             The text.content is mandatory for this button.
 * @param actionUrl A string representing the URL to be triggered when the button is clicked.
 *                  This is a mandatory field.
 * @param borWidth Optional. An integer representing the width of the button's border in dp.
 * @param borColor Optional. A string representing the color of the button's border in hex format (e.g., "#FFFFFF").
 *                 If not provided, the default border color is transparent.
 */
@Composable
fun AEPButtonComposable(
    interactId: String,
    text: AEPText,
    actionUrl: String,
    borWidth: Int? = null,
    borColor: String? = null
) {
    // Check for mandatory fields
    require(interactId.isNotEmpty()) { "interactId is mandatory for AEPButtonComposable" }
    require(text.content != null) { "text.content is mandatory for AEPButtonComposable" }
    require(actionUrl.isNotEmpty()) { "actionUrl is mandatory for AEPButtonComposable" }

    // Set optional border properties
    val borderColor = borColor?.let { Color(parseColor(it)) } ?: Color.Transparent

    // Composable for Button
    Button(
        onClick = {
            // Handle button click logic
            println("Button clicked with action URL: $actionUrl")
        },
        border = borWidth?.let { BorderStroke(it.dp, borderColor) },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray),
        modifier = Modifier.padding(8.dp) // Modifier with padding to avoid layout issues
    ) {
        // Use AEPTextComposable to render the button text
        AEPTextComposable(
            content = text.content.orEmpty(),
            clr = text.clr,
            align = text.align,
            font = text.font
        )
    }
}
