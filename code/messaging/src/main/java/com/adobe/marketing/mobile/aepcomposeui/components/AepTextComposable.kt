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

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * A composable function that displays a text element with customizable properties.
 *
 * @param model The [AepText] model that contains the text properties.
 * @param textStyle The [AepTextStyle] to be applied to the text element.
 */
@Composable
internal fun AepTextComposable(
    model: AepText,
    textStyle: AepTextStyle = AepTextStyle()
) {
    if (model.content.isBlank()) {
        return
    }
    Text(
        text = model.content,
        style = textStyle.textStyle ?: TextStyle(),
        modifier = textStyle.modifier ?: Modifier,
        overflow = textStyle.overflow ?: TextOverflow.Ellipsis,
        softWrap = textStyle.softWrap ?: true,
        maxLines = textStyle.maxLines ?: AepUIConstants.TEXT_MAX_LINES,
        minLines = textStyle.minLines ?: 1
    )
}

/**
 * Preview for testing the [AepTextComposable] with various inputs.
 * This function showcases different variations of text properties, including colors,
 * alignments, fonts, and edge cases.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    device = "spec:width=1080px,height=2340px,dpi=440"
)
@Composable
internal fun AepTextComposablePreview() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Basic usage
        AepTextComposable(AepText(content = "Basic Text"))

        // Color variations
        AepTextComposable(AepText(content = "Red Text"))
        AepTextComposable(AepText(content = "Green Text"))
        AepTextComposable(AepText(content = "Blue Text"))
        AepTextComposable(AepText(content = "Invalid Color")) // Test invalid color

        // Alignment variations
        AepTextComposable(AepText(content = "Left Aligned"))
        AepTextComposable(AepText(content = "Center Aligned"))
        AepTextComposable(AepText(content = "Right Aligned"))
        AepTextComposable(AepText(content = "Invalid Alignment")) // Test invalid alignment

        // Font variations
        AepTextComposable(AepText(content = "Large Text"))
        AepTextComposable(AepText(content = "Small Text"))
        AepTextComposable(AepText(content = "Bold Text"))
        AepTextComposable(AepText(content = "Italic Text"))
        AepTextComposable(AepText(content = "Bold Italic Text"))

        // Combination of properties
        AepTextComposable(
            AepText(
                content = "Complex Styling",
            )
        )

        // Edge cases
        AepTextComposable(AepText(content = "Empty String")) // Providing valid content instead of empty
        AepTextComposable(AepText(content = "Very Long Text ".repeat(20))) // Very long text
        AepTextComposable(AepText(content = "Special Characters: !@#$%^&*()_+{}[]|\\:;\"'<>,.?/"))
        AepTextComposable(AepText(content = "Multi\nLine\nText")) // Multi-line text

        // Null values for optional parameters
        AepTextComposable(AepText(content = "Null Color"))
        AepTextComposable(AepText(content = "Null Alignment"))
        AepTextComposable(AepText(content = "Null Font"))

        // Extreme font sizes
        AepTextComposable(AepText(content = "Tiny Text"))
        AepTextComposable(AepText(content = "Huge Text"))
    }
}
