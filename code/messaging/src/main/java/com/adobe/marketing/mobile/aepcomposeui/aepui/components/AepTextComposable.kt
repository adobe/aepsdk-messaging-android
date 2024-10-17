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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepFont

/**
 * A composable function that displays a text element with customizable properties.
 *
 * @param content The text content to be displayed. This is a mandatory field.
 * @param clr Optional. A string representing the color of the text in hex format (e.g., "#FFFFFF").
 *            If not provided or invalid, defaults to black.
 * @param align Optional. A string representing the alignment of the text.
 *              Accepted values are "left", "center", and "right". Defaults to start alignment.
 * @param font Optional. An [AepFont] object containing font properties such as size, weight, and style.
 */
@Composable
fun AepTextComposable(
    content: String,
    clr: String? = null,
    align: String? = null,
    font: AepFont? = null
) {
    // TODO: Implement the AEPTextComposable
    // Here code added as placeholder for reference, actual implementation is pending
    require(content.isNotEmpty()) { "content is mandatory for AEPTextComposable" }

    // Parse color from string to Color object, with exception handling
    val textColor = try {
        clr?.let { Color(android.graphics.Color.parseColor(it)) } ?: androidx.compose.ui.graphics.Color.Black
    } catch (e: IllegalArgumentException) {
        Color.Black // Fallback to black if the color string is invalid
    }

    // Determine text alignment safely
    val textAlign = when (align?.lowercase()) {
        "left" -> TextAlign.Left
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        else -> TextAlign.Start
    }

    // Set font properties
    val fontFamily = FontFamily.Default // Use default or map from `font.name` if needed
    val fontSize = (font?.size ?: 14).sp
    val fontWeight = when (font?.weight?.lowercase()) {
        "bold" -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val fontStyle = if (font?.style?.contains("italic") == true) {
        FontStyle.Italic
    } else {
        FontStyle.Normal
    }

    // Text Composable
    Text(
        text = content,
        color = textColor,
        textAlign = textAlign,
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        modifier = Modifier.fillMaxWidth()
    )
}
