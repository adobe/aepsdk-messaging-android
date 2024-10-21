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

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.merge
import com.adobe.marketing.mobile.aepcomposeui.aepui.utils.UIUtils.getColor
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepButton
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepColor
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepFont
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText

/**
 * A composable function that displays a button element with customizable properties.
 *
 * @param defaultButtonStyle The default [AepButtonStyle] to be applied to the button element.
 * @param overriddenButtonStyle The [AepButtonStyle] provided by the app that overrides the default button style.
 * @param defaultButtonTextStyle The default [AepTextStyle] to be applied to the button text.
 * @param overriddenButtonTextStyle The [AepTextStyle] provided by the app that overrides the default button text style.
 * @param onClick Method that is called when this button is clicked
 */
@Composable
internal fun AepButton.Composable(
    defaultButtonStyle: AepButtonStyle? = null,
    overriddenButtonStyle: AepButtonStyle? = null,
    defaultButtonTextStyle: AepTextStyle? = null,
    overriddenButtonTextStyle: AepTextStyle? = null,
    onClick: () -> Unit
) {
    // Set button border properties
    val border = borderWidth?.let {
        BorderStroke(
            it.dp,
            borderColor?.getColor() ?: Color.Unspecified
        )
    }

    // Set button color
    val colors = backgroundColour?.let { ButtonDefaults.buttonColors(backgroundColour.getColor()) }

    val mergedAepButtonStyle = defaultButtonStyle
        .merge(AepButtonStyle(border = border, colors = colors))
        .merge(overriddenButtonStyle)

    // Button Composable
    Button(
        onClick = onClick,
        modifier = mergedAepButtonStyle?.modifier ?: Modifier,
        enabled = mergedAepButtonStyle?.enabled ?: true,
        elevation = mergedAepButtonStyle?.elevation ?: ButtonDefaults.buttonElevation(),
        shape = mergedAepButtonStyle?.shape ?: ButtonDefaults.shape,
        border = mergedAepButtonStyle?.border,
        colors = mergedAepButtonStyle?.colors ?: ButtonDefaults.buttonColors(),
    ) {
        // Use AEPText.Composable for button text
        AepText(
            content = text.content,
            color = text.color,
            align = text.align,
            font = text.font
        ).Composable(
            defaultStyle = defaultButtonTextStyle,
            overriddenStyle = overriddenButtonTextStyle
        )
    }
}

/**
 * Preview for [AepButton.Composable].
 * This function creates a sample button using predefined schema data for demonstration
 * purposes. It showcases how the button will appear with various styling options.
 */
@Preview(showBackground = true)
@Composable
internal fun AepButtonComposablePreview() {
    // Render the AepButtonComposable with the properties from AepButton
    AepButton(
        id = "button1",
        text = AepText(
            "Click Me",
            color = AepColor("#FF0000CC"),
            align = "center",
            font = AepFont(
                name = "Arial",
                size = 16,
                weight = "bold",
                style = listOf("italic")
            )
        ),
        actionUrl = "https://www.adobe.com",
        borderWidth = 2.0f,
        borderColor = AepColor("#0FE608AC")
    ).Composable(onClick = {})
}
