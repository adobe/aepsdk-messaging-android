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

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigPictureStyle
import androidx.test.core.app.ApplicationProvider
import com.adobe.marketing.mobile.MessagingPushPayload
import com.adobe.marketing.mobile.services.caching.CacheResult
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CompletableFuture
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MessagingRichPushBuilderTests {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var payload: MessagingPushPayload

    private val data = mapOf("key" to "value")

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkConstructor(MessageAssetDownloader::class)
        every { anyConstructed<MessageAssetDownloader>().downloadAssetCollection() } just Runs

        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk()

        mockkStatic(MessagingPushUtils::class)
        every { MessagingPushUtils.getCachedRichMediaFileUri(any()) } returns Uri.parse("mockUri")
        every { MessagingPushUtils.download(any()) } returns mockk()

        context = spyk(ApplicationProvider.getApplicationContext())
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        payload = mockk(relaxed = true) {
            every { channelId } returns "test_channel" // Changed to provide a channel ID
            every { title } returns "Test Title"
            every { body } returns "Test Body"
            every { badgeCount } returns 0
            every { notificationPriority } returns NotificationCompat.PRIORITY_DEFAULT
            every { notificationVisibility } returns NotificationCompat.VISIBILITY_PUBLIC
            every { messageId } returns "mockMessageId"
            every { actionButtons } returns null
            every { actionType } returns null
            every { icon } returns null
            every { sound } returns null
            every { data } returns this@MessagingRichPushBuilderTests.data
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `verify big picture style applied to notification when gif url is provided`() {
        // Setup
        val gifUrl = "https://example.com/animation.gif"
        every { payload.imageUrl } returns gifUrl

        // Setup a cached asset to be returned via a completable future
        every { MessagingPushUtils.downloadAndCacheAsset(any(), any(), any()) } returns CompletableFuture.supplyAsync({ mockk<CacheResult>() })

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify notification was created
        assertNotNull(notification)

        // Verify BigPicture style was applied
        verify { anyConstructed<NotificationCompat.Builder>().setStyle(any(BigPictureStyle::class)) }
    }

    @Test
    fun `verify notification still created when gif download fails`() {
        // Setup
        val gifUrl = "https://example.com/animation.gif"
        every { payload.imageUrl } returns gifUrl

        // Setup a null cached asset to be returned via a completable future
        every { MessagingPushUtils.downloadAndCacheAsset(any(), any(), any()) } returns CompletableFuture.supplyAsync({ null })

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify notification was created
        assertNotNull(notification)

        // Verify big picture style was not applied
        verify(exactly = 0) { anyConstructed<NotificationCompat.Builder>().setStyle(any(BigPictureStyle::class)) }
    }

    @Test
    fun `verify notification still created with no image when the payload url is null`() {
        // Setup
        every { payload.imageUrl } returns null

        // Setup a cached asset to be returned via a completable future
        every { MessagingPushUtils.downloadAndCacheAsset(any(), any(), any()) } returns CompletableFuture.supplyAsync({ mockk<CacheResult>() })

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify notification was created
        assertNotNull(notification)

        // Verify big picture style was not applied
        verify(exactly = 0) { anyConstructed<NotificationCompat.Builder>().setStyle(any(BigPictureStyle::class)) }
    }

    @Test
    fun `verify notification still created with no image when the cached asset metadata is null`() {
        // Setup
        val gifUrl = "https://example.com/animation.gif"
        every { payload.imageUrl } returns gifUrl
        every { MessagingPushUtils.getCachedRichMediaFileUri(any()) } returns null

        // Setup a cached asset to be returned via a completable future
        every { MessagingPushUtils.downloadAndCacheAsset(any(), any(), any()) } returns CompletableFuture.supplyAsync({ mockk<CacheResult>() })

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify notification was created
        assertNotNull(notification)

        // Verify big picture style was not applied
        verify(exactly = 0) { anyConstructed<NotificationCompat.Builder>().setStyle(any(BigPictureStyle::class)) }
    }

    @Test
    fun `test non-gif image handling`() {
        // Setup
        val imageUrl = "https://example.com/image.jpg"
        every { payload.imageUrl } returns imageUrl

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify MessageAssetDownloader was not used
        verify(exactly = 0) { anyConstructed<MessageAssetDownloader>().downloadAssetCollection() }

        // Verify MessagingPushUtils was used as a regular image was provided
        verify(exactly = 1) { MessagingPushUtils.download(eq("https://example.com/image.jpg")) }

        // Verify notification was created
        assertNotNull(notification)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `test gif is displayed as a bitmap on API 33 and below`() {
        // Setup
        val gifUrl = "https://example.com/animation.gif"
        every { payload.imageUrl } returns gifUrl

        // Execute
        val notification = MessagingPushBuilder.build(payload, context)

        // Verify MessageAssetDownloader was not used
        verify(exactly = 0) { anyConstructed<MessageAssetDownloader>().downloadAssetCollection() }

        // Verify MessagingPushUtils was used as api level is below 33
        verify(exactly = 1) { MessagingPushUtils.download(eq(gifUrl)) }

        // Verify notification was created
        assertNotNull(notification)
    }
}
