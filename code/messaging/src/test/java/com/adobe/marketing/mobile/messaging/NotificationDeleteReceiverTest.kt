package com.adobe.marketing.mobile.messaging

import android.content.Context
import android.content.Intent
import com.adobe.marketing.mobile.Messaging
import org.junit.Test
import org.mockito.Mockito.*

class NotificationDeleteReceiverTest {

    @Test
    fun `onReceive should call Messaging handleNotificationResponse with dismiss action`() {
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)
        val receiver = NotificationDeleteReceiver()

        // Mock static method
        mockStatic(Messaging::class.java).use { messagingMock ->
            receiver.onReceive(context, intent)
            messagingMock.verify {
                Messaging.handleNotificationResponse(intent, false, "Dismiss")
            }
        }
    }
}
