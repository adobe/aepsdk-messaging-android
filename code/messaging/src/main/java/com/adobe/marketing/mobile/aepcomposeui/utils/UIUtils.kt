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
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.services.Log
import java.io.IOException
import java.io.InputStream
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
    fun downloadImage(url: String?): Bitmap? {
        var bitmap: Bitmap? = null
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        try {
            val imageUrl = URL(url)
            connection = imageUrl.openConnection() as HttpURLConnection
            inputStream = connection.inputStream
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            Log.warning(
                AepUIConstants.LOG_TAG,
                SELF_TAG,
                "Failed to download push notification image from url (%s). Exception: %s",
                url,
                e.message
            )
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    Log.warning(
                        AepUIConstants.LOG_TAG,
                        SELF_TAG,
                        "IOException during closing Input stream while push notification image" +
                            " from url (%s). Exception: %s ",
                        url,
                        e.message
                    )
                }
            }

            connection?.disconnect()
        }

        return bitmap
    }
}
