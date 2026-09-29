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

import android.content.Context
import android.content.Intent
import com.adobe.marketing.mobile.Messaging
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationInteractionReceiverTest {

    /** Records the actions it handles. */
    private class RecordingHandler(
        override val runsInBackground: Boolean = false,
        private val failure: Throwable? = null
    ) : NotificationInteractionHandler {
        val handled = mutableListOf<String?>()

        override fun handle(context: Context, intent: Intent) {
            handled.add(intent.action)
            failure?.let { throw it }
        }
    }

    /** Runs tasks on the calling thread and counts them. */
    private class CountingExecutor : Executor {
        var executed = 0

        override fun execute(command: Runnable) {
            executed++
            command.run()
        }
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `dispatches to the handler registered for the intent action`() {
        val dismiss = RecordingHandler()
        val other = RecordingHandler()
        val executor = CountingExecutor()
        val receiver = NotificationInteractionReceiver(mapOf("dismiss" to dismiss, "other" to other), executor)

        receiver.onReceive(context, Intent("dismiss"))

        assertEquals(listOf("dismiss"), dismiss.handled)
        assertTrue(other.handled.isEmpty())
        assertEquals(0, executor.executed)
    }

    @Test
    fun `runs background handlers on the executor`() {
        val slow = RecordingHandler(runsInBackground = true)
        val executor = CountingExecutor()
        val receiver = NotificationInteractionReceiver(mapOf("slow" to slow), executor)

        receiver.onReceive(context, Intent("slow"))

        assertEquals(1, executor.executed)
        assertEquals(listOf("slow"), slow.handled)
    }

    @Test
    fun `a failing background handler does not crash the receiver`() {
        val slow = RecordingHandler(runsInBackground = true, failure = IllegalStateException("boom"))
        val receiver = NotificationInteractionReceiver(mapOf("slow" to slow), CountingExecutor())

        receiver.onReceive(context, Intent("slow"))

        assertEquals(listOf("slow"), slow.handled)
    }

    @Test
    fun `ignores unrecognized actions`() {
        val dismiss = RecordingHandler()
        val receiver = NotificationInteractionReceiver(mapOf("dismiss" to dismiss), CountingExecutor())

        receiver.onReceive(context, Intent("unknown"))

        assertTrue(dismiss.handled.isEmpty())
    }

    @Test
    fun `ignores a null context`() {
        val dismiss = RecordingHandler()
        val receiver = NotificationInteractionReceiver(mapOf("dismiss" to dismiss), CountingExecutor())

        receiver.onReceive(null, Intent("dismiss"))

        assertTrue(dismiss.handled.isEmpty())
    }

    @Test
    fun `onReceive should call Messaging handleNotificationResponse with dismiss action`() {
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(MessagingPushConstants.NotificationAction.DISMISSED)
        val receiver = NotificationInteractionReceiver()

        // Mock static method
        mockStatic(Messaging::class.java).use { messagingMock ->
            receiver.onReceive(context, intent)
            messagingMock.verify {
                Messaging.handleNotificationResponse(intent, false, "Dismiss")
            }
        }
    }

    @Test
    fun `onReceive should track an interaction by its actionId`() {
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(MessagingPushConstants.NotificationAction.INTERACTION)
        `when`(intent.getStringExtra("actionId")).thenReturn("reply")
        val receiver = NotificationInteractionReceiver()

        mockStatic(Messaging::class.java).use { messagingMock ->
            receiver.onReceive(context, intent)
            messagingMock.verify {
                Messaging.handleNotificationResponse(intent, false, "reply")
            }
        }
    }

    @Test
    fun `onReceive with null intent should not call Messaging handleNotificationResponse`() {
        val context = mock(Context::class.java)
        val receiver = NotificationInteractionReceiver()
        val intent = mock(Intent::class.java)

        mockStatic(Messaging::class.java).use { messagingMock ->
            receiver.onReceive(context, null)
            messagingMock.verify(
                { Messaging.handleNotificationResponse(intent, false, "Dismiss") },
                never()
            )
        }
    }
}
