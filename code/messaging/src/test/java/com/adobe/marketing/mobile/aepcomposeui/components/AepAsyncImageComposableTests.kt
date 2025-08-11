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

package com.adobe.marketing.mobile.aepcomposeui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.messaging.ContentCardImageManager
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class AepAsyncImageComposableTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockImageUrl = "https://example.com/mock_image.png"
    private val mockDarkImageUrl = "https://example.com/mock_dark_image.png"
    private val mockAepImage = AepImage(url = mockImageUrl, darkUrl = mockDarkImageUrl)
    private lateinit var mockLightBitmap: Bitmap
    private lateinit var mockDarkBitmap: Bitmap

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Before
    fun setUp() {
        RuntimeEnvironment.setQualifiers(qualifier)
        MockKAnnotations.init(this)
        mockLightBitmap = BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().resources,
            android.R.drawable.ic_menu_report_image
        )
        mockDarkBitmap = BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().resources,
            android.R.drawable.ic_menu_info_details
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Test AepAsyncImageComposable shows progress indicator while loading`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(eq(mockImageUrl), any(), any())
        } just Runs

        // test
        composeTestRule.setContent {
            AepAsyncImageComposable(image = mockAepImage)
        }

        // verify and capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage("build/outputs/roborazzi/AepAsyncImageComposable_Loading_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepAsyncImageComposable shows image on success`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(eq(mockImageUrl), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }
        var downloadedBitmap: Bitmap? = null

        // test
        composeTestRule.setContent {
            AepAsyncImageComposable(
                image = mockAepImage,
                onSuccess = { downloadedBitmap = it }
            )
        }

        // verify and capture screenshot
        assertNotNull(downloadedBitmap)
        assertEquals(mockLightBitmap, downloadedBitmap)
        composeTestRule.onRoot()
            .captureRoboImage("build/outputs/roborazzi/AepAsyncImageComposable_Success_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepAsyncImageComposable shows dark theme image on success`() {
        // setup
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cfg = ctx.resources.configuration
        cfg.uiMode = Configuration.UI_MODE_NIGHT_YES or
            (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)
        ctx.resources.updateConfiguration(cfg, ctx.resources.displayMetrics)
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(eq(mockDarkImageUrl), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }
        var downloadedBitmap: Bitmap? = null

        // test
        composeTestRule.setContent {
            TestTheme(useDarkTheme = true) {
                AepAsyncImageComposable(
                    image = mockAepImage,
                    onSuccess = { downloadedBitmap = it }
                )
            }
        }

        // verify
        assertNotNull(downloadedBitmap)
        assertEquals(mockDarkBitmap, downloadedBitmap)
        composeTestRule.onRoot()
            .captureRoboImage("build/outputs/roborazzi/AepAsyncImageComposable_SuccessDark_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepAsyncImageComposable handles image load failure`() {
        // setup
        mockkObject(ContentCardImageManager)
        val exception = RuntimeException("Image loading failed")
        every {
            ContentCardImageManager.getContentCardImageBitmap(eq(mockImageUrl), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.failure(exception))
        }
        var onErrorCalled: Throwable? = null

        // test
        composeTestRule.setContent {
            AepAsyncImageComposable(
                image = mockAepImage,
                imageStyle = AepImageStyle(
                    modifier = Modifier.testTag("AepImageComposable")
                ),
                onError = { onErrorCalled = it }
            )
        }

        // verify
        assertNotNull(onErrorCalled)
        assertEquals(exception, onErrorCalled)
        composeTestRule
            .onAllNodes(hasTestTag("AepImageComposable"))
            .assertCountEquals(0)
    }

    @Test
    fun `Test AepAsyncImageComposable handles null image url`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } just Runs

        // test
        composeTestRule.setContent {
            AepAsyncImageComposable(image = AepImage(null))
        }

        // verify
        verify(exactly = 0) {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        }
        composeTestRule
            .onAllNodes(hasTestTag("AepImageComposable"))
            .assertCountEquals(0)
    }

    @Test
    fun `Test AepAsyncImageComposable handles null image`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } just Runs

        // test
        composeTestRule.setContent {
            AepAsyncImageComposable(
                image = null,
                imageStyle = AepImageStyle(
                    modifier = Modifier.testTag("AepImageComposable")
                )
            )
        }

        // verify
        verify(exactly = 0) {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        }
        composeTestRule
            .onAllNodes(hasTestTag("AepImageComposable"))
            .assertCountEquals(0)
    }
}
