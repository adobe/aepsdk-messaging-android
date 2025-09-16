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

package com.adobe.marketing.mobile.messaging

import android.content.Intent
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

internal object MessagingNotificationEventsManager {

    private val callbacks = CopyOnWriteArrayList<WeakReference<AdobeCallbackWithError<MessagingNotificationEvent>>>()

    fun addCallback(callback: AdobeCallbackWithError<MessagingNotificationEvent>) {
        for (ref in callbacks) {
            val c = ref.get()
            if (c === callback) return
        }
        callbacks.add(WeakReference(callback))
        cleanupCallbacks()
    }

    fun removeCallback(callback: AdobeCallbackWithError<MessagingNotificationEvent>) {
        for (ref in callbacks) {
            val c = ref.get()
            if (c == null || c === callback) {
                callbacks.remove(ref)
            }
        }
        cleanupCallbacks()
    }

    fun sendNotificationCallback(
        intent: Intent,
        customActionId: String?
    ) {
        val data = getDataFromIntent(intent)
        dispatchEvent(
            MessagingNotificationEvent(
                customActionId,
                data
            )
        )
    }

    private fun getDataFromIntent(
        intent: Intent
    ): Map<String, String> {
        val data = HashMap<String, String>()
        val bundle = intent.extras ?: return emptyMap()
        val bundleKeys = bundle.keySet()
        for (dataKey in bundleKeys) {
            val value = try {
                bundle.getString(dataKey)
            } catch (_: Exception) {
                null
            }
            if (value != null) {
                data[dataKey] = value
            }
        }
        return data as Map<String, String>
    }

    private fun dispatchEvent(event: MessagingNotificationEvent) {
        for (ref in callbacks) {
            ref.get()?.call(event)
        }
        cleanupCallbacks()
    }

    private fun dispatchError(error: AdobeError) {
        for (ref in callbacks) {
            ref.get()?.fail(error)
        }
        cleanupCallbacks()
    }

    private fun cleanupCallbacks() {
        callbacks.removeAll { it.get() == null }
    }
}
