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

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MessagingPushPayload
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.PushNotificationListener
import com.adobe.marketing.mobile.plugin.IPushTemplateTrackingProvider
import com.adobe.marketing.mobile.plugin.IUiTemplatePlugin
import com.adobe.marketing.mobile.plugin.PushInteraction
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import com.google.firebase.messaging.RemoteMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Host-side flow of a push-template notification, end to end on the JVM: [MessagingService] receives
 * the push, [MessagingPushBuilder] hands it to the registered [IUiTemplatePlugin], the notification is
 * posted, and each interaction [PendingIntent] the plugin obtained from [MessagingPushTrackingProvider]
 * is delivered to the component it targets ([MessagingPushTrackerActivity] or
 * [NotificationInteractionReceiver]).
 *
 * The plugin is a stand-in for aepsdk-ui-android; the assertions cover what Messaging owns: the data
 * handed to the plugin, the posted id, the tracking events, the listener callbacks, cancellation and
 * navigation. They mirror the on-device push-template smoke cases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PushTemplateFlowTest {

    /** Builds a notification the way the UI plugin does: every PendingIntent comes from the provider. */
    private class FakeTemplatePlugin : IUiTemplatePlugin {
        var returnNull = false
        var messageData: Map<String, String>? = null
        var trackingProvider: IPushTemplateTrackingProvider? = null
        var built: Notification? = null

        override fun buildPushTemplateNotification(
            messageData: Map<String, String>,
            trackingProvider: IPushTemplateTrackingProvider
        ): Notification? {
            this.messageData = messageData
            this.trackingProvider = trackingProvider
            if (returnNull) return null

            val context = RuntimeEnvironment.getApplication()
            built = NotificationCompat.Builder(context, messageData.getValue("adb_channel_id"))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(messageData["adb_title"])
                .setContentIntent(
                    trackingProvider.getPendingIntent(
                        PushInteraction(CONTENT_CLICK, actionUri = messageData["adb_uri"])
                    )
                )
                .setDeleteIntent(trackingProvider.getPendingIntent(PushInteraction(DISMISS)))
                .addAction(
                    0,
                    DEEPLINK_BUTTON,
                    trackingProvider.getPendingIntent(
                        PushInteraction(BUTTON_CLICK, actionUri = DEEPLINK_URI, actionId = DEEPLINK_BUTTON)
                    )
                )
                .addAction(
                    0,
                    OPEN_APP_BUTTON,
                    trackingProvider.getPendingIntent(
                        PushInteraction(BUTTON_CLICK, actionId = OPEN_APP_BUTTON)
                    )
                )
                .setAutoCancel(true)
                .build()
            return built
        }
    }

    /** Records the listener callbacks in the order they arrive. */
    private class RecordingListener : PushNotificationListener {
        val callbacks = mutableListOf<String>()
        val messageIds = mutableListOf<String?>()

        override fun onNotificationReceived(payload: MessagingPushPayload) {
            callbacks.add("received")
            messageIds.add(payload.messageId)
        }

        override fun onNotificationOpened(payload: MessagingPushPayload, actionButtonId: String?) {
            callbacks.add("opened(btn=$actionButtonId)")
            messageIds.add(payload.messageId)
        }

        override fun onNotificationDismissed(payload: MessagingPushPayload) {
            callbacks.add("dismissed")
            messageIds.add(payload.messageId)
        }
    }

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val notificationManager: NotificationManager
        get() = app.getSystemService(NotificationManager::class.java)
    private val postedId = MessagingPushUtils.getNotificationId(MESSAGE_ID)

    private lateinit var mobileCore: MockedStatic<MobileCore>
    private lateinit var serviceProvider: MockedStatic<ServiceProvider>
    private lateinit var hostActivity: Activity
    private var plugin: FakeTemplatePlugin? = FakeTemplatePlugin()
    private val listener = RecordingListener()
    private val events = mutableListOf<Event>()

    @Before
    fun setup() {
        hostActivity = Robolectric.buildActivity(Activity::class.java).setup().get()

        val appContextService = mock(AppContextService::class.java)
        `when`(appContextService.applicationContext).thenReturn(app)
        `when`(appContextService.currentActivity).thenReturn(hostActivity)
        val services = mock(ServiceProvider::class.java)
        `when`(services.appContextService).thenReturn(appContextService)
        serviceProvider = mockStatic(ServiceProvider::class.java)
        serviceProvider.`when`<ServiceProvider> { ServiceProvider.getInstance() }.thenReturn(services)

        // Core is stubbed at the event boundary: dispatched events are recorded and the plugin
        // registry returns the current plugin.
        mobileCore = mockStatic(
            MobileCore::class.java,
            Answer { invocation ->
                when (invocation.method.name) {
                    "dispatchEvent", "dispatchEventWithResponseCallback" -> {
                        events.add(invocation.arguments[0] as Event)
                        null
                    }
                    "getPlugin" -> plugin
                    else -> RETURNS_DEFAULTS.answer(invocation)
                }
            }
        )

        // Core is already initialized in the app process, so receive tracking runs immediately.
        setSelfInitTried(true)
        Messaging.setPushNotificationListener(listener)
    }

    @After
    fun tearDown() {
        Messaging.setPushNotificationListener(null)
        setSelfInitTried(false)
        mobileCore.close()
        serviceProvider.close()
    }

    @Test
    fun `template push is built by the plugin and posted under the id its intents are scoped to`() {
        receive()

        val plugin = requireNotNull(plugin)
        val data = requireNotNull(plugin.messageData)
        assertEquals(MESSAGE_ID, data["messageId"])
        assertEquals(postedId.toString(), data["notificationId"])
        assertEquals("AJOPushChannel", data["adb_channel_id"])
        assertEquals("Title", data["adb_title"])

        assertSame(plugin.built, posted())
        // the provider's intents are scoped to the id the notification was posted with
        val contentIntent = shadowOf(posted()!!.contentIntent).savedIntent
        assertEquals(postedId.toString(), contentIntent.data?.authority)

        val received = trackingEvent("pushTracking.receive")
        assertEquals(MESSAGE_ID, received.eventData["messageId"])
        assertEquals(XDM, received.eventData["adobe_xdm"])
        assertEquals(listOf("received"), listener.callbacks)
    }

    @Test
    fun `body tap tracks an app open and opens the deeplink`() {
        receive(mapOf("adb_uri" to BODY_URI))

        val tracker = tap(posted()!!.contentIntent)

        val opened = trackingEvent("pushTracking.applicationOpened")
        assertEquals(MESSAGE_ID, opened.eventData["messageId"])
        assertEquals(true, opened.eventData["applicationOpened"])
        assertEquals(XDM, opened.eventData["adobe_xdm"])
        assertNull(opened.eventData["actionId"])
        assertEquals(listOf("received", "opened(btn=null)"), listener.callbacks)
        assertEquals(listOf(MESSAGE_ID, MESSAGE_ID), listener.messageIds)
        assertOpenedUri(tracker, BODY_URI)
    }

    @Test
    fun `body tap without a uri tracks an app open and opens the app`() {
        receive()

        val tracker = tap(posted()!!.contentIntent)

        assertEquals(true, trackingEvent("pushTracking.applicationOpened").eventData["applicationOpened"])
        assertEquals(listOf("received", "opened(btn=null)"), listener.callbacks)
        assertOpenedApp(tracker)
    }

    @Test
    fun `deeplink button tracks the custom action, cancels the notification and opens the deeplink`() {
        receive()

        val tracker = tap(action(DEEPLINK_BUTTON))

        val clicked = trackingEvent("pushTracking.customAction")
        assertEquals(DEEPLINK_BUTTON, clicked.eventData["actionId"])
        assertEquals(true, clicked.eventData["applicationOpened"])
        assertEquals(MESSAGE_ID, clicked.eventData["messageId"])
        assertEquals(listOf("received", "opened(btn=$DEEPLINK_BUTTON)"), listener.callbacks)
        assertNull(posted())
        assertOpenedUri(tracker, DEEPLINK_URI)
    }

    @Test
    fun `open-app button tracks the custom action, cancels the notification and opens the app`() {
        receive()

        val tracker = tap(action(OPEN_APP_BUTTON))

        val clicked = trackingEvent("pushTracking.customAction")
        assertEquals(OPEN_APP_BUTTON, clicked.eventData["actionId"])
        assertEquals(true, clicked.eventData["applicationOpened"])
        assertEquals(listOf("received", "opened(btn=$OPEN_APP_BUTTON)"), listener.callbacks)
        assertNull(posted())
        assertOpenedApp(tracker)
    }

    @Test
    fun `dismiss tracks a Dismiss custom action without opening the app`() {
        receive()

        tap(posted()!!.deleteIntent)

        val dismissed = trackingEvent("pushTracking.customAction")
        assertEquals("Dismiss", dismissed.eventData["actionId"])
        assertEquals(false, dismissed.eventData["applicationOpened"])
        assertEquals(MESSAGE_ID, dismissed.eventData["messageId"])
        assertEquals(listOf("received", "dismissed"), listener.callbacks)
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `a template the plugin cannot build falls back to the basic notification`() {
        requireNotNull(plugin).returnNull = true

        receive()

        assertNotNull(requireNotNull(plugin).messageData)
        val notification = requireNotNull(posted())
        assertNotSame(requireNotNull(plugin).built, notification)
        assertEquals("AJOPushChannel", notification.channelId)
        assertEquals(MESSAGE_ID, trackingEvent("pushTracking.receive").eventData["messageId"])
        assertEquals(listOf("received"), listener.callbacks)
    }

    @Test
    fun `without a registered plugin the basic notification is posted`() {
        plugin = null

        receive()

        val notification = requireNotNull(posted())
        assertEquals("AJOPushChannel", notification.channelId)
        assertEquals(MESSAGE_ID, trackingEvent("pushTracking.receive").eventData["messageId"])
        assertEquals(listOf("received"), listener.callbacks)
    }

    private fun receive(extra: Map<String, String> = emptyMap()) {
        val remoteMessage = RemoteMessage.Builder("sender@fcm.googleapis.com")
            .setMessageId(MESSAGE_ID)
            .setData(
                mapOf(
                    "_xdm" to XDM,
                    "adb_template_type" to "ajo_basic",
                    "adb_version" to "1",
                    "adb_title" to "Title",
                    "adb_body" to "Body"
                ) + extra
            )
            .build()
        assertTrue(MessagingService.handleRemoteMessage(app, remoteMessage))
    }

    private fun posted(): Notification? = shadowOf(notificationManager).getNotification(postedId)

    private fun action(title: String): PendingIntent =
        requireNotNull(posted()).actions.single { it.title == title }.actionIntent

    /**
     * Delivers [pendingIntent] the way the system would: activity intents launch
     * [MessagingPushTrackerActivity], broadcasts go to [NotificationInteractionReceiver].
     *
     * @return the launched tracker activity, or `null` for a broadcast
     */
    private fun tap(pendingIntent: PendingIntent): Activity? {
        val shadow = shadowOf(pendingIntent)
        val intent = shadow.savedIntent
        return if (shadow.isActivityIntent) {
            assertEquals(MessagingPushTrackerActivity::class.java.name, intent.component?.className)
            Robolectric.buildActivity(MessagingPushTrackerActivity::class.java, intent).create().get()
        } else {
            assertEquals(NotificationInteractionReceiver::class.java.name, intent.component?.className)
            NotificationInteractionReceiver().onReceive(app, intent)
            null
        }
    }

    private fun trackingEvent(eventType: String): Event =
        events.single { it.eventData?.get("eventType") == eventType }

    private fun assertOpenedUri(tracker: Activity?, uri: String) {
        val started = shadowOf(requireNotNull(tracker)).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse(uri), started.data)
    }

    private fun assertOpenedApp(tracker: Activity?) {
        val started = shadowOf(requireNotNull(tracker)).nextStartedActivity
        assertEquals(hostActivity.javaClass.name, started.component?.className)
    }

    private fun setSelfInitTried(value: Boolean) {
        MessagingService::class.java.getDeclaredField("selfInitTried").apply {
            isAccessible = true
            setBoolean(null, value)
        }
    }

    private companion object {
        const val MESSAGE_ID = "flow-message-1"
        const val XDM = "{\"cjm\":{\"_experience\":{}}}"
        const val BODY_URI = "myapp://home"
        const val DEEPLINK_URI = "myapp://offer"
        const val DEEPLINK_BUTTON = "Deeplink"
        const val OPEN_APP_BUTTON = "Open App"
        const val CONTENT_CLICK = "content_click"
        const val BUTTON_CLICK = "button_click"
        const val DISMISS = "dismiss"
    }
}
