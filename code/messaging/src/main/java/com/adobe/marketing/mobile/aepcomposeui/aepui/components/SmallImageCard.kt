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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.aepui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.aepui.utils.UIAction
import com.adobe.marketing.mobile.aepcomposeui.interactions.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver

/**
 * Composable function that renders a small image card UI.
 *
 * @param ui The small image AEP UI to be rendered.
 * @param style The style to be applied to the small image UI.
 * @param observer An optional observer that listens to UI events.
 */
@Composable
internal fun SmallImageCard(
    ui: SmallImageUI,
    style: SmallImageUIStyle,
    observer: AepUIEventObserver?,
) {

    LaunchedEffect(key1 = Unit) {
        observer?.onEvent(UIEvent.Display(ui))
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            observer?.onEvent(UIEvent.Dismiss(ui))
        }
    }

    Card(
        modifier = Modifier
            .clickable {
                observer?.onEvent(UIEvent.Interact(ui, UIAction.CLICK))
            }
    ) {
        Row {
            // TODO - Add image support
            Column {
                ui.getTemplate().title.Composable(
                    defaultStyle = style.defaultTitleTextStyle,
                    overriddenStyle = style.titleAepTextStyle
                )
                ui.getTemplate().body?.Composable(
                    defaultStyle = style.defaultBodyTextStyle,
                    overriddenStyle = style.bodyAepTextStyle
                )
                Row {
                    ui.getTemplate().buttons?.forEachIndexed { index, button ->
                        button.Composable(
                            defaultButtonStyle = null,
                            overriddenButtonStyle = style.buttonAepButtonStyle[index],
                            defaultButtonTextStyle = style.defaultButtonTextStyle,
                            overriddenButtonTextStyle = style.buttonAepTextStyle[index],
                            onClick = {
                                observer?.onEvent(UIEvent.Interact(ui, UIAction.CLICK))
                            }
                        )
                    }
                }
            }
        }
    }
}
