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

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.adobe.marketing.mobile.aepcomposeui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * A composable function that displays a button element with customizable properties.
 *
 * @param model The [AepButton] model that contains the button properties.
 * @param onClick Method that is called when this button is clicked
 * @param defaultButtonStyle The default [AepButtonStyle] to be applied to the button element.
 * @param overridingButtonStyle The [AepButtonStyle] provided by the app that overrides the default button style.
 * @param defaultButtonTextStyle The default [AepTextStyle] to be applied to the button text.
 * @param overridingButtonTextStyle The [AepTextStyle] provided by the app that overrides the default button text style.
 */
@Composable
internal fun AepButtonComposable(
    model: AepButton,
    onClick: () -> Unit,
    defaultButtonStyle: AepButtonStyle? = null,
    overridingButtonStyle: AepButtonStyle? = null,
    defaultButtonTextStyle: AepTextStyle? = null,
    overridingButtonTextStyle: AepTextStyle? = null,
) {
    val mergedStyle = AepButtonStyle.merge(defaultButtonStyle, overridingButtonStyle)
    Button(
        onClick = onClick,
        modifier = mergedStyle.modifier ?: Modifier,
        enabled = mergedStyle.enabled ?: true,
        shape = mergedStyle.shape ?: ButtonDefaults.shape,
        colors = mergedStyle.colors ?: ButtonDefaults.buttonColors(),
        elevation = mergedStyle.elevation,
        border = mergedStyle.border,
        contentPadding = mergedStyle.contentPadding ?: ButtonDefaults.ContentPadding,
    ) {
        // Use AEPText.Composable for button text
        AepTextComposable(
            model.text,
            defaultStyle = defaultButtonTextStyle,
            overridingStyle = overridingButtonTextStyle
        )
    }
}

/**
 * Preview for [AepTextComposable].
 * This function creates a sample button using predefined schema data for demonstration
 * purposes. It showcases how the button will appear with various styling options.
 */
@Preview(showBackground = true)
@Composable
internal fun AepButtonComposablePreview() {
    // Render the AepButtonComposable with the properties from AepButton
    AepButtonComposable(
        AepButton(
            id = "button1",
            text = AepText(
                "Click Me",
            ),
            actionUrl = "https://www.adobe.com",
        ),
        onClick = {}
    )
}
