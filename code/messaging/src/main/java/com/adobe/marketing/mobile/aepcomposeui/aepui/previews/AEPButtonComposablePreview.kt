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

package com.adobe.marketing.mobile.aepcomposeui.aepui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.Composable
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepButton
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepColor
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepFont
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText

/**
 * Preview for [AepButton.Composable].
 * This function creates a sample button using predefined schema data for demonstration
 * purposes. It showcases how the button will appear with various styling options.
 */
@Preview(showBackground = true)
@Composable
fun PreviewButton() {
    // Render the AEPButtonComposable with the properties from aepButton
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
