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

import android.app.PendingIntent
import android.content.Intent
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.NotificationAction
import com.adobe.marketing.mobile.plugin.PushInteraction
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MessagingPushTrackingProviderTest {

    private lateinit var serviceProviderMock: MockedStatic<ServiceProvider>
    private lateinit var provider: MessagingPushTrackingProvider

    private val messageId = "message-1"
    private val data = mapOf(
        "adb_template_type" to "ajo_basic",
        "adb_title" to "Title",
        "_xdm" to "{\"cjm\":{}}",
        "messageId" to messageId,
        "notificationId" to "42"
    )

    @Before
    fun setup() {
        val appContextService = mock(AppContextService::class.java)
        `when`(appContextService.applicationContext).thenReturn(RuntimeEnvironment.getApplication())
        val serviceProvider = mock(ServiceProvider::class.java)
        `when`(serviceProvider.appContextService).thenReturn(appContextService)
        serviceProviderMock = mockStatic(ServiceProvider::class.java)
        serviceProviderMock.`when`<ServiceProvider> { ServiceProvider.getInstance() }.thenReturn(serviceProvider)
        provider = MessagingPushTrackingProvider(messageId, data)
    }

    @After
    fun tearDown() {
        serviceProviderMock.close()
    }

    private fun savedIntent(pendingIntent: PendingIntent?): Intent {
        assertNotNull(pendingIntent)
        return shadowOf(pendingIntent).savedIntent
    }

    @Test
    fun `content_click routes to the tracker activity as OPENED with tracking extras`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("content_click", actionUri = "myapp://home"))

        assertTrue(shadowOf(pendingIntent).isActivityIntent)
        val intent = savedIntent(pendingIntent)
        assertEquals(NotificationAction.OPENED, intent.action)
        assertEquals(MessagingPushTrackerActivity::class.java.name, intent.component?.className)
        assertEquals("myapp://home", intent.getStringExtra("actionUri"))
        assertEquals(messageId, intent.getStringExtra("messageId"))
        assertEquals("{\"cjm\":{}}", intent.getStringExtra("adobe_xdm"))
        assertEquals("Title", intent.getStringExtra("adb_title"))
    }

    @Test
    fun `button_click routes to the tracker activity as BUTTON_CLICKED with actionId`() {
        val intent = savedIntent(
            provider.getPendingIntent(PushInteraction("button_click", actionId = "Open"))
        )

        assertEquals(NotificationAction.BUTTON_CLICKED, intent.action)
        assertEquals("Open", intent.getStringExtra("actionId"))
    }

    @Test
    fun `dismiss routes to the receiver as DISMISSED`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("dismiss"))

        assertTrue(shadowOf(pendingIntent).isBroadcastIntent)
        val intent = savedIntent(pendingIntent)
        assertEquals(NotificationAction.DISMISSED, intent.action)
        assertEquals(NotificationInteractionReceiver::class.java.name, intent.component?.className)
    }

    @Test
    fun `a mutable PendingIntent is created when the interaction asks for one`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("dismiss", mutablePendingIntent = true))

        assertTrue(shadowOf(pendingIntent).flags and PendingIntent.FLAG_MUTABLE != 0)
    }

    @Test
    fun `tracking intents are immutable by default`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("dismiss"))

        assertTrue(shadowOf(pendingIntent).flags and PendingIntent.FLAG_IMMUTABLE != 0)
    }

    @Test
    fun `unknown type with an actionUri falls back to OPENED`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("future_type", actionUri = "https://adobe.com"))

        assertTrue(shadowOf(pendingIntent).isActivityIntent)
        assertEquals(NotificationAction.OPENED, savedIntent(pendingIntent).action)
    }

    @Test
    fun `unknown type without an actionUri falls back to a silent INTERACTION`() {
        val pendingIntent = provider.getPendingIntent(PushInteraction("future_type", actionId = "x"))

        assertTrue(shadowOf(pendingIntent).isBroadcastIntent)
        assertEquals(NotificationAction.INTERACTION, savedIntent(pendingIntent).action)
    }

    @Test
    fun `identity uri is deterministic and scoped to the notification id`() {
        val intent = savedIntent(
            provider.getPendingIntent(
                PushInteraction("button_click", actionId = "next", templateExtras = mapOf("b" to "2", "a" to "1"))
            )
        )

        assertEquals("adbpush://42/button_click?id=next&s.a=1&s.b=2", intent.dataString)
    }

    @Test
    fun `the same interaction resolves to the same PendingIntent`() {
        val first = provider.getPendingIntent(PushInteraction("button_click", actionId = "Open"))
        val second = MessagingPushTrackingProvider(messageId, data)
            .getPendingIntent(PushInteraction("button_click", actionId = "Open"))

        assertEquals(first, second)
    }

    @Test
    fun `interactions differing only in template state resolve to different PendingIntents`() {
        val left = provider.getPendingIntent(PushInteraction("button_click", templateExtras = mapOf("adb_index" to "0")))
        val right = provider.getPendingIntent(PushInteraction("button_click", templateExtras = mapOf("adb_index" to "2")))

        assertNotEquals(left, right)
    }

    @Test
    fun `each button gets its own PendingIntent that keeps its own action`() {
        val deeplink = provider.getPendingIntent(
            PushInteraction("button_click", actionUri = "myapp://offer", actionId = "Deeplink")
        )
        val openApp = provider.getPendingIntent(PushInteraction("button_click", actionId = "Open App"))

        // creating the second button must not overwrite the first one's extras
        assertNotEquals(deeplink, openApp)
        val deeplinkIntent = savedIntent(deeplink)
        assertEquals("Deeplink", deeplinkIntent.getStringExtra("actionId"))
        assertEquals("myapp://offer", deeplinkIntent.getStringExtra("actionUri"))
        val openAppIntent = savedIntent(openApp)
        assertEquals("Open App", openAppIntent.getStringExtra("actionId"))
        assertFalse(openAppIntent.hasExtra("actionUri"))
    }

    @Test
    fun `the same interaction on two notifications resolves to different PendingIntents`() {
        val otherData = data + mapOf("messageId" to "message-2", "notificationId" to "43")
        val first = provider.getPendingIntent(PushInteraction("content_click"))
        val second = MessagingPushTrackingProvider("message-2", otherData)
            .getPendingIntent(PushInteraction("content_click"))

        assertNotEquals(first, second)
        assertEquals(messageId, savedIntent(first).getStringExtra("messageId"))
        assertEquals("message-2", savedIntent(second).getStringExtra("messageId"))
    }

    @Test
    fun `content_click without an actionUri carries no actionUri so the tracker opens the app`() {
        val intent = savedIntent(provider.getPendingIntent(PushInteraction("content_click")))

        assertFalse(intent.hasExtra("actionUri"))
        assertFalse(intent.hasExtra("actionId"))
    }

    @Test
    fun `tracking intents carry the push-to-in-app id and skip empty data values`() {
        val trackingProvider = MessagingPushTrackingProvider(
            messageId,
            data + mapOf("adb_iam_id" to "iam-1", "adb_body" to "")
        )

        val intent = savedIntent(trackingProvider.getPendingIntent(PushInteraction("content_click")))

        assertEquals("iam-1", intent.getStringExtra("adb_iam_id"))
        assertFalse(intent.hasExtra("adb_body"))
    }

    @Test
    fun `notificationIdFor prefers the reserved key and falls back to the messageId hash`() {
        assertEquals(42, MessagingPushTrackingProvider.notificationIdFor(messageId, data))
        assertEquals(
            messageId.hashCode(),
            MessagingPushTrackingProvider.notificationIdFor(messageId, mapOf("adb_title" to "Title"))
        )
    }

    @Test
    fun `returns null when there is no application context`() {
        `when`(ServiceProvider.getInstance().appContextService.applicationContext).thenReturn(null)

        assertNull(provider.getPendingIntent(PushInteraction("content_click")))
    }
}
