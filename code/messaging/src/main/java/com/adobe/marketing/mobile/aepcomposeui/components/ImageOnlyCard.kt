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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle

/**
 * Composable function that renders a small image card UI.
 *
 * @param ui The small image AEP UI to be rendered.
 * @param style The style to be applied to the small image UI.
 * @param observer An optional observer that listens to UI events.
 */
@Composable
fun ImageOnlyCard(
    ui: ImageOnlyUI,
    style: ImageOnlyUIStyle,
    observer: AepUIEventObserver?,
) {
    var isImageDownloadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(ui.getTemplate().id) {
        observer?.onEvent(UIEvent.Display(ui))
    }

    // If the image download has failed, we do not render the card.
    if (isImageDownloadFailed) return

    AepCardComposable(
        cardStyle = AepCardStyle(
            shape = RoundedCornerShape(0.dp)
        ),
        onClick = {
            observer?.onEvent(
                UIEvent.Interact(
                    ui,
                    UIAction.Click(
                        AepUIConstants.InteractionID.CARD_CLICKED,
                        ui.getTemplate().actionUrl
                    )
                )
            )
        }
    ) {
        Box {
            AepAsyncImage(
                image = ui.getTemplate().image,
                imageStyle = style.imageStyle,
                onError = {
                    isImageDownloadFailed = true
                }
            )
            AepDismissButton(
                modifier = Modifier.align(style.dismissButtonAlignment),
                dismissIcon = ui.getTemplate().dismissBtn,
                style = style.dismissButtonStyle,
                onClick = { observer?.onEvent(UIEvent.Dismiss(ui)) },
            )
        }
    }
}
