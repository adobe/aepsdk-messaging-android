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

package com.adobe.marketing.mobile.aepcomposeui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.services.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object UIUtils {

    private const val SELF_TAG = "UIUtils"

    /**
     * Downloads the image from the given URL.
     *
     * @param url the URL of the image to download.
     * @return the downloaded image as a [Bitmap].
     */
    // TODO: This method is repeated in Messaging, maybe it should be moved to a common place
    fun downloadImage(url: String?): Bitmap? {
        var connection: HttpURLConnection? = null

        return try {
            val imageUrl = URL(url)
            connection = imageUrl.openConnection() as HttpURLConnection

            with(connection) {
                inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: IOException) {
            Log.warning(
                AepUIConstants.LOG_TAG,
                SELF_TAG,
                "Failed to download push notification image from url (%s). Exception: %s",
                url,
                e.message
            )
            null
        } finally {
            connection?.disconnect()
        }
    }

    @Composable
    fun Modifier.shimmerEffect(
        shimmerColor1: Color? = null,
        shimmerColor2: Color? = null
    ): Modifier = composed {
        var size by remember { mutableStateOf(IntSize.Zero) }

        // Infinite shimmer animation transition
        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            ),
            label = "ShimmerOffsetX"
        )

        val shimmerColorLight = shimmerColor1 ?: MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
        val shimmerColorDark = shimmerColor2 ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

        this
            .onGloballyPositioned { coordinates ->
                size = coordinates.size // Capture the composable's size
            }
            .drawWithCache {
                // Only re-draw the gradient when the shimmer position updates
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        shimmerColorLight,
                        shimmerColorDark,
                        shimmerColorLight
                    ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
                )

                onDrawBehind {
                    drawRect(
                        brush = gradient,
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                }
            }
    }
}
