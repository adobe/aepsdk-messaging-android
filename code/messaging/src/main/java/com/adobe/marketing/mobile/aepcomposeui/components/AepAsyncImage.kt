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
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.messaging.ContentCardImageManager

/**
 * A composable function that downloads and caches an image from the URL specified in [AepImage] and
 * displays it. A progress indicator is shown while the image is being loaded.
 * If the image fails to load, no view is rendered and the `onError` callback is invoked with the error details.
 *
 * @param image The [AepImage] to be displayed.
 * @param imageStyle The [AepImageStyle] to be applied to the image.
 * @param onSuccess Callback invoked when the image is successfully loaded.
 * @param onError Callback invoked when there is an error loading the image.
 */
@Composable
internal fun AepAsyncImage(
    image: AepImage,
    imageStyle: AepImageStyle = AepImageStyle(),
    onSuccess: (Bitmap) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val imageUrl = if (isSystemInDarkTheme() && image.darkUrl != null)
        image.darkUrl else image.url
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        ContentCardImageManager.getContentCardImageBitmap(imageUrl) {
            it.onSuccess { bitmap ->
                imageBitmap = bitmap
                isLoading = false
                onSuccess(bitmap)
            }
            it.onFailure { throwable ->
                // todo - confirm default image bitmap to be used here
                isLoading = false
                onError(throwable)
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = imageStyle.modifier ?: Modifier
                .size(AepUIConstants.DefaultAepUIStyle.IMAGE_WIDTH.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(AepUIConstants.DefaultAepUIStyle.IMAGE_PROGRESS_SPINNER_SIZE.dp),
                strokeWidth = 4.dp
            )
        }
    } else {
        imageBitmap?.let {
            AepImage(
                content = BitmapPainter(it.asImageBitmap()),
                imageStyle = imageStyle
            )
        }
    }
}
