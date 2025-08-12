/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton

/**
 * A composable function to render a row of equally spaced, same-sized buttons.
 *
 * @param buttons A list of [AepButton] objects to be displayed in the row.
 * @param buttonsStyle An array of [AepButtonStyle] to be applied to each button.
 * @param rowStyle The [AepRowStyle] to be applied to the row.
 * @param onClick Callback function to be invoked when a button is clicked.
 */
@Composable
internal fun AepButtonRow(
    buttons: List<AepButton>?,
    buttonsStyle: Array<AepButtonStyle>,
    rowStyle: AepRowStyle = AepRowStyle(),
    onClick: (AepButton) -> Unit = {}
) {
    buttons?.let { buttonList ->
        AepRow(
            rowStyle = rowStyle
        ) {
            buttonList.forEachIndexed { index, button ->
                AepButton(
                    model = button,
                    onClick = { onClick(button) },
                    buttonStyle = buttonsStyle[index].apply {
                        modifier = (modifier ?: Modifier).then(Modifier.weight(1f))
                    }
                )
            }
        }
    }
}
