/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.NotificationAction
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.TemplateIntent
import com.adobe.marketing.mobile.plugin.IPushTemplateTrackingProvider
import com.adobe.marketing.mobile.plugin.IUiTemplatePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PushTemplateRerenderHandlerTest {

    private lateinit var context: Context
    private val notification = Notification()
    private val tracked = mutableListOf<String?>()
    private val posted = mutableListOf<Pair<Int, Notification>>()
    private val tracker = PushResponseTracker { _, actionId -> tracked.add(actionId) }
    private val poster = NotificationPoster { _, id, n -> posted.add(id to n) }

    /** Records the message data it is asked to build from and returns [result]. */
    private class FakePlugin(private val result: Notification?) : IUiTemplatePlugin {
        var messageData: Map<String, String>? = null

        override fun buildPushTemplateNotification(
            messageData: Map<String, String>,
            trackingProvider: IPushTemplateTrackingProvider
        ): Notification? {
            this.messageData = messageData
            return result
        }
    }

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun handler(plugin: IUiTemplatePlugin?) =
        PushTemplateRerenderHandler(tracker, { plugin }, poster)

    private fun rerenderIntent(
        actionId: String? = "next",
        payload: Bundle? = Bundle().apply {
            putString("messageId", "message-1")
            putString("notificationId", "42")
            putString("adb_title", "Title")
            putString("adb_index", "0")
        }
    ): Intent {
        val intent = Intent(NotificationAction.RERENDER)
        payload?.let { intent.putExtra(TemplateIntent.PUSH_PAYLOAD, it) }
        intent.putExtra(
            TemplateIntent.TEMPLATE_STATE,
            Bundle().apply {
                putString("adb_index", "2")
                putString("adb_input", "state")
            }
        )
        actionId?.let { intent.putExtra("actionId", it) }
        return intent
    }

    @Test
    fun `runs in the background`() {
        assertTrue(handler(FakePlugin(notification)).runsInBackground)
    }

    @Test
    fun `rebuilds from the merged data and posts under the same notification id`() {
        val plugin = FakePlugin(notification)
        val intent = rerenderIntent()
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder("adb_input").build()),
            intent,
            Bundle().apply { putCharSequence("adb_input", "typed") }
        )

        handler(plugin).handle(context, intent)

        val messageData = plugin.messageData!!
        assertEquals("Title", messageData["adb_title"])
        assertEquals("message-1", messageData["messageId"])
        assertEquals("42", messageData["notificationId"])
        // merge order: payload, then template state, then input results
        assertEquals("2", messageData["adb_index"])
        assertEquals("typed", messageData["adb_input"])
        assertEquals(42, posted.single().first)
        assertSame(notification, posted.single().second)
    }

    @Test
    fun `tracks the gesture when an actionId is present`() {
        handler(FakePlugin(notification)).handle(context, rerenderIntent(actionId = "next"))

        assertEquals(listOf("next"), tracked)
    }

    @Test
    fun `does not track the gesture without an actionId`() {
        handler(FakePlugin(notification)).handle(context, rerenderIntent(actionId = null))

        assertTrue(tracked.isEmpty())
    }

    @Test
    fun `posts nothing when the plugin returns null`() {
        handler(FakePlugin(null)).handle(context, rerenderIntent())

        assertTrue(posted.isEmpty())
    }

    @Test
    fun `posts nothing when no plugin is registered`() {
        handler(null).handle(context, rerenderIntent())

        assertTrue(posted.isEmpty())
    }

    @Test
    fun `ignores an intent without message data`() {
        val plugin = FakePlugin(notification)

        handler(plugin).handle(context, rerenderIntent(payload = null))

        assertNull(plugin.messageData)
        assertTrue(tracked.isEmpty())
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `ignores an intent without a message id`() {
        val plugin = FakePlugin(notification)

        handler(plugin).handle(
            context,
            rerenderIntent(payload = Bundle().apply { putString("adb_title", "Title") })
        )

        assertNull(plugin.messageData)
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `request merges payload, then template state, then input results`() {
        val request = PushTemplateRerenderRequest(
            messageId = "message-1",
            payload = mapOf("a" to "payload", "b" to "payload", "c" to "payload"),
            templateState = mapOf("b" to "state", "c" to "state"),
            inputResults = mapOf("c" to "input")
        )

        assertEquals(mapOf("a" to "payload", "b" to "state", "c" to "input"), request.messageData())
    }

    @Test
    fun `request notification id falls back to the message id hash`() {
        val request = PushTemplateRerenderRequest("message-1", emptyMap(), emptyMap(), emptyMap())

        assertEquals("message-1".hashCode(), request.notificationId)
    }
}
