/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messagingsample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ImageDownloader {
    companion object {
        fun getImage(url: String): Bitmap? {
            var bitmap: Bitmap? = null

            val latch = CountDownLatch(1)
            Thread {
                try {
                    val urlImage = URL(url)
                    val connection = urlImage.openConnection() as HttpURLConnection
                    val inputStream = connection.inputStream
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    latch.countDown()
                } catch (e: MalformedURLException) {
                    Log.e("ImageDownloader", "Malformed url exception: ${e.localizedMessage}")
                } catch (e: IOException) {
                    Log.e("ImageDownloader", "IO Exception: ${e.localizedMessage}")
                }
            }.start()
            latch.await(2, TimeUnit.SECONDS)
            return bitmap
        }
    }
}