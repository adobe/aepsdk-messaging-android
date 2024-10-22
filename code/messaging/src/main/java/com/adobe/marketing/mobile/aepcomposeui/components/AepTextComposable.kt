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
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepFont
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * A composable function that displays a text element with customizable properties.
 *
 * @param model The [AepText] model that contains the text properties.
 * @param defaultStyle The default [AepTextStyle] to be applied to the text element.
 * @param overridingStyle The [AepTextStyle] provided by the app that overrides the default text style.
 */
@Composable
internal fun AepTextComposable(
    model: AepText,
    defaultStyle: AepTextStyle? = null,
    overridingStyle: AepTextStyle? = null
) {
    val mergedStyle = AepTextStyle.merge(defaultStyle, overridingStyle)
    Text(
        text = model.content,
        style = mergedStyle.textStyle ?: TextStyle(),
        modifier = mergedStyle.modifier ?: Modifier,
        overflow = mergedStyle.overflow ?: TextOverflow.Clip,
        softWrap = mergedStyle.softWrap ?: true,
        maxLines = mergedStyle.maxLines ?: Int.MAX_VALUE,
        minLines = mergedStyle.minLines ?: 1
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
        AepTextComposable(AepText(content = "Red Text", color = AepColor("#FF0000")))
        AepTextComposable(AepText(content = "Green Text", color = AepColor("#00FF00")))
        AepTextComposable(AepText(content = "Blue Text", color = AepColor("#0000FF")))
        AepTextComposable(AepText(content = "Invalid Color", color = AepColor("invalid"))) // Test invalid color

        // Alignment variations
        AepTextComposable(AepText(content = "Left Aligned", align = "left"))
        AepTextComposable(AepText(content = "Center Aligned", align = "center"))
        AepTextComposable(AepText(content = "Right Aligned", align = "right"))
        AepTextComposable(AepText(content = "Invalid Alignment", align = "invalid")) // Test invalid alignment

        // Font variations
        AepTextComposable(AepText(content = "Large Text", font = AepFont(size = 24)))
        AepTextComposable(AepText(content = "Small Text", font = AepFont(size = 10)))
        AepTextComposable(AepText(content = "Bold Text", font = AepFont(weight = "bold")))
        AepTextComposable(AepText(content = "Italic Text", font = AepFont(style = listOf("italic"))))
        AepTextComposable(AepText(content = "Bold Italic Text", font = AepFont(weight = "bold", style = listOf("italic"))))

        // Combination of properties
        AepTextComposable(
            AepText(
                content = "Complex Styling",
                color = AepColor("#800080"),
                align = "center",
                font = AepFont(size = 18, weight = "bold", style = listOf("italic"))
            )
        )

        // Edge cases
        AepTextComposable(AepText(content = "Empty String")) // Providing valid content instead of empty
        AepTextComposable(AepText(content = "Very Long Text ".repeat(20))) // Very long text
        AepTextComposable(AepText(content = "Special Characters: !@#$%^&*()_+{}[]|\\:;\"'<>,.?/"))
        AepTextComposable(AepText(content = "Multi\nLine\nText")) // Multi-line text

        // Null values for optional parameters
        AepTextComposable(AepText(content = "Null Color", color = null))
        AepTextComposable(AepText(content = "Null Alignment", align = null))
        AepTextComposable(AepText(content = "Null Font", font = null))

        // Extreme font sizes
        AepTextComposable(AepText(content = "Tiny Text", font = AepFont(size = 1)))
        AepTextComposable(AepText(content = "Huge Text", font = AepFont(size = 100)))
    }
}
