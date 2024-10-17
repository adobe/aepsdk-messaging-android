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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun SmallImageCard(
    ui: SmallImageUI,
    style: SmallImageUIStyle,
    observer: AepUIEventObserver?,
) {

    // TODO - Implement the SmallImageCard composable
    // Here code added as placeholder for reference, actual implementation is pending

    LaunchedEffect(key1 = Unit) {
        observer?.onEvent(UIEvent.Display(ui))
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            observer?.onEvent(UIEvent.Dismiss(ui))
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                observer?.onEvent(UIEvent.Interact(ui, UIAction.CLICK))
            },
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO - Add image support
            Spacer(modifier = Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                ui.getTemplate().title.let {
                    Text(
                        text = it?.content ?: "",
                        style = style.getTitleTextStyle(ui.getTemplate()),
                    )
                }
                Text(
                    text = ui.getTemplate().description?.content ?: "",
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}
