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

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var isLoading by remember { mutableStateOf(true) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val imageUrl = if (isSystemInDarkTheme() && ui.getTemplate().image?.darkUrl != null)
        ui.getTemplate().image?.darkUrl else ui.getTemplate().image?.url

    LaunchedEffect(ui.getTemplate().id) {
        observer?.onEvent(UIEvent.Display(ui))
        if (imageUrl.isNullOrBlank()) {
            isLoading = false
        } else {
            imageBitmap = withContext(Dispatchers.IO) {
                UIUtils.downloadImage(imageUrl)
            }
            isLoading = false
        }
    }

    AepCardComposable(
        cardStyle = style.cardStyle,
        onClick = {
            observer?.onEvent(UIEvent.Interact(ui, UIAction.Click(ui.getTemplate().id, ui.getTemplate().actionUrl)))
        }
    ) {
        Box {
            ui.getTemplate().dismissBtn?.let {
                AepIconComposable(
                    drawableId = it.drawableId,
                    // todo check if we can remember this calculation so that it is not repeated for recompositions
                    style = style.dismissButtonStyle.apply {
                        modifier = (modifier ?: Modifier).align(style.dismissButtonAlignment)
                    },
                    onClick = {
                        observer?.onEvent(UIEvent.Dismiss(ui))
                    }
                )
            }

            AepRowComposable(
                rowStyle = style.rootRowStyle
            ) {
                imageBitmap?.let {
                    AepImageComposable(
                        content = BitmapPainter(it.asImageBitmap()),
                        style = style.imageStyle
                    )
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .size(AepUIConstants.SmallImageCard.DefaultStyle.IMAGE_WIDTH.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                }
                AepColumnComposable(
                    columnStyle = style.textColumnStyle
                ) {
                    ui.getTemplate().title.let {
                        AepTextComposable(
                            model = it,
                            textStyle = style.titleAepTextStyle
                        )
                    }
                    ui.getTemplate().body?.let {
                        AepTextComposable(
                            model = it,
                            textStyle = style.bodyAepTextStyle
                        )
                    }
                    AepRowComposable(
                        rowStyle = style.buttonRowStyle
                    ) {
                        ui.getTemplate().buttons?.forEachIndexed { index, button ->
                            AepButtonComposable(
                                button,
                                onClick = {
                                    observer?.onEvent(UIEvent.Interact(ui, UIAction.Click(button.id, button.actionUrl)))
                                },
                                buttonStyle = style.buttonStyle[index].first.apply {
                                    modifier = (modifier ?: Modifier).then(Modifier.weight(1f))
                                },
                                buttonTextStyle = style.buttonStyle[index].second
                            )
                        }
                    }
                }
            }
        }
    }
}
