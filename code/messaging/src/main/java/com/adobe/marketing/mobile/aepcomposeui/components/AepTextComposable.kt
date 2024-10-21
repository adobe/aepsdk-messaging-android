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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.merge
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepFont
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils.getColor

/**
 * A composable function that displays a text element with customizable properties.
 *
 * @param defaultStyle The default [AepTextStyle] to be applied to the text element.
 * @param overriddenStyle The [AepTextStyle] provided by the app that overrides the default text style.
 */
@Composable
internal fun AepText.Composable(
    defaultStyle: AepTextStyle? = null,
    overriddenStyle: AepTextStyle? = null
) {
    // Convert server color from string to Color object
    val textColor = color?.getColor()

    // Convert server text alignment from string to TextAlign object
    val textAlign = when (align?.lowercase()) {
        "left" -> TextAlign.Left
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        else -> null
    }

    // Convert server font properties from string to respective font objects
    // TODO map font string to FontFamily
    // val fontFamily = null
    val fontSize = (font?.size)?.sp
    val fontWeight = when (font?.weight?.lowercase()) {
        "bold" -> FontWeight.Bold
        else -> null
    }
    val fontStyle = if (font?.style?.contains("italic") == true) {
        FontStyle.Italic
    } else {
        null
    }

    // Merge all text styles together
    val mergedAepTextStyle =
        defaultStyle
            .merge(textColor?.let { AepTextStyle(textStyle = TextStyle(color = it)) })
            .merge(textAlign?.let { AepTextStyle(textStyle = TextStyle(textAlign = it)) })
            .merge(fontSize?.let { AepTextStyle(textStyle = TextStyle(fontSize = it)) })
            .merge(
                AepTextStyle(
                    textStyle = TextStyle(
                        fontWeight = fontWeight,
                        fontStyle = fontStyle
                    )
                )
            )
            .merge(fontStyle?.let { AepTextStyle(textStyle = TextStyle(fontStyle = it)) })
            .merge(overriddenStyle)

    // Text Composable
    Text(
        text = content,
        style = mergedAepTextStyle?.textStyle ?: TextStyle(),
        modifier = mergedAepTextStyle?.modifier ?: Modifier,
        overflow = mergedAepTextStyle?.overflow ?: TextOverflow.Clip,
        softWrap = mergedAepTextStyle?.softWrap ?: true,
        maxLines = mergedAepTextStyle?.maxLines ?: Int.MAX_VALUE,
        minLines = mergedAepTextStyle?.minLines ?: 1
    )
}

/**
 * Preview for testing the [AepText.Composable] with various inputs.
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
        AepText(content = "Basic Text").Composable()

        // Color variations
        AepText(content = "Red Text", color = AepColor("#FF0000")).Composable()
        AepText(content = "Green Text", color = AepColor("#00FF00")).Composable()
        AepText(content = "Blue Text", color = AepColor("#0000FF")).Composable()
        AepText(content = "Invalid Color", color = AepColor("invalid")).Composable() // Test invalid color

        // Alignment variations
        AepText(content = "Left Aligned", align = "left").Composable()
        AepText(content = "Center Aligned", align = "center").Composable()
        AepText(content = "Right Aligned", align = "right").Composable()
        AepText(content = "Invalid Alignment", align = "invalid").Composable() // Test invalid alignment

        // Font variations
        AepText(content = "Large Text", font = AepFont(size = 24)).Composable()
        AepText(content = "Small Text", font = AepFont(size = 10)).Composable()
        AepText(content = "Bold Text", font = AepFont(weight = "bold")).Composable()
        AepText(content = "Italic Text", font = AepFont(style = listOf("italic"))).Composable()
        AepText(content = "Bold Italic Text", font = AepFont(weight = "bold", style = listOf("italic"))).Composable()

        // Combination of properties
        AepText(
            content = "Complex Styling",
            color = AepColor("#800080"),
            align = "center",
            font = AepFont(size = 18, weight = "bold", style = listOf("italic"))
        ).Composable()

        // Edge cases
        AepText(content = "Empty String").Composable() // Providing valid content instead of empty
        AepText(content = "Very Long Text ".repeat(20)).Composable() // Very long text
        AepText(content = "Special Characters: !@#$%^&*()_+{}[]|\\:;\"'<>,.?/").Composable()
        AepText(content = "Multi\nLine\nText").Composable() // Multi-line text

        // Null values for optional parameters
        AepText(content = "Null Color", color = null).Composable()
        AepText(content = "Null Alignment", align = null).Composable()
        AepText(content = "Null Font", font = null).Composable()

        // Extreme font sizes
        AepText(content = "Tiny Text", font = AepFont(size = 1)).Composable()
        AepText(content = "Huge Text", font = AepFont(size = 100)).Composable()
    }
}
