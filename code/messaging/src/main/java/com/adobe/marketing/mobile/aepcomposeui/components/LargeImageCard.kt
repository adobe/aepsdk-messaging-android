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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle

/**
 * Composable function that renders a large image card UI.
 *
 * @param ui The large image AEP UI to be rendered.
 * @param style The style to be applied to the large image UI.
 * @param observer An optional observer that listens to UI events.
 */
@Composable
fun LargeImageCard(
    ui: LargeImageUI,
    style: LargeImageUIStyle,
    observer: AepUIEventObserver?,
) {
    LaunchedEffect(ui.getTemplate().id) {
        observer?.onEvent(UIEvent.Display(ui))
    }

    AepCardComposable(
        cardStyle = style.cardStyle,
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
            AepColumnComposable(
                columnStyle = style.rootColumnStyle
            ) {
                AepAsyncImage(
                    image = ui.getTemplate().image,
                    imageStyle = style.imageStyle
                )
                AepColumnComposable(
                    columnStyle = style.textColumnStyle
                ) {
                    ui.getTemplate().title.let {
                        AepTextComposable(
                            model = it,
                            textStyle = style.titleTextStyle
                        )
                    }
                    ui.getTemplate().body?.let {
                        AepTextComposable(
                            model = it,
                            textStyle = style.bodyTextStyle
                        )
                    }
                    AepButtonRow(
                        buttons = ui.getTemplate().buttons,
                        buttonsStyle = style.buttonStyle,
                        rowStyle = style.buttonRowStyle,
                        onClick = { button ->
                            observer?.onEvent(
                                UIEvent.Interact(
                                    ui,
                                    UIAction.Click(button.id, button.actionUrl)
                                )
                            )
                        }
                    )
                }
            }
            AepDismissButton(
                modifier = Modifier.align(style.dismissButtonAlignment),
                dismissIcon = ui.getTemplate().dismissBtn,
                style = style.dismissButtonStyle,
                onClick = { observer?.onEvent(UIEvent.Dismiss(ui)) },
            )
        }
    }
}
