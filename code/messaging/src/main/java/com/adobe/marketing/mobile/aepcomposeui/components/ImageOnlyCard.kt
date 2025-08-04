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

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
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
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.messaging.ContentCardImageManager

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
    var isLoading by remember { mutableStateOf(true) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val imageUrl = if (isSystemInDarkTheme() && ui.getTemplate().image?.darkUrl != null)
        ui.getTemplate().image?.darkUrl else ui.getTemplate().image?.url

    LaunchedEffect(ui.getTemplate().id) {
        observer?.onEvent(UIEvent.Display(ui))
        if (imageUrl.isNullOrBlank()) {
            isLoading = false
        } else {
            ContentCardImageManager.getContentCardImageBitmap(imageUrl) {
                it.onSuccess { bitmap ->
                    imageBitmap = bitmap
                    isLoading = false
                }
                it.onFailure {
                    // todo - confirm default image bitmap to be used here
                    // imageBitmap = contentCardManager.getDefaultImageBitmap()
                    isLoading = false
                }
            }
        }
    }
    Box {
        AepCardComposable(
            cardStyle = style.cardStyle,
            onClick = {
                observer?.onEvent(UIEvent.Interact(ui, UIAction.Click(AepUIConstants.InteractionID.CARD_CLICKED, ui.getTemplate().actionUrl)))
            }
        ) {
            imageBitmap?.let {
                AepImageComposable(
                    content = BitmapPainter(it.asImageBitmap()),
                    imageStyle = style.imageStyle
                )
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(AepUIConstants.DefaultStyle.IMAGE_WIDTH.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }
        ui.getTemplate().dismissBtn?.let {
            AepIconComposable(
                drawableId = it.drawableId,
                // todo check if we can remember this calculation so that it is not repeated for recompositions
                iconStyle = style.dismissButtonStyle.apply {
                    modifier = (modifier ?: Modifier)
                        .align(style.dismissButtonAlignment)
                        .clickable {
                            observer?.onEvent(UIEvent.Dismiss(ui))
                        }
                }
            )
        }
    }
}
