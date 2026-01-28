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

package com.adobe.marketing.mobile.aepcomposeui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.messaging.ContentCardImageManager
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
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
@Config(sdk = [31])
class EmptyInboxTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockEmptyMessage = AepText("Your inbox is empty")
    private val mockImageUrl = "https://example.com/empty_inbox.png"
    private val mockDarkImageUrl = "https://example.com/empty_inbox_dark.png"
    private val mockEmptyImage = AepImage(url = mockImageUrl, darkUrl = mockDarkImageUrl)
    private lateinit var mockLightBitmap: Bitmap

    private val mockCustomTextStyle = AepTextStyle(
        modifier = Modifier.size(200.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFF0065db),
            fontSize = 18.sp
        )
    )

    private val mockCustomImageStyle = AepImageStyle(
        modifier = Modifier.size(100.dp),
        contentDescription = "Empty inbox illustration"
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    private lateinit var mockDarkBitmap: Bitmap

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
    fun `Test EmptyInbox with message only`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            EmptyInbox(
                emptyMessage = mockEmptyMessage
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_MessageOnly_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with message only in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                EmptyInbox(
                    emptyMessage = mockEmptyMessage
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_MessageOnlyDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with image only`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            EmptyInbox(
                emptyImage = mockEmptyImage
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_ImageOnly_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with image only in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                EmptyInbox(
                    emptyImage = mockEmptyImage
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_ImageOnlyDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with both message and image`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            EmptyInbox(
                emptyMessage = mockEmptyMessage,
                emptyImage = mockEmptyImage
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_BothMessageAndImage_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with both message and image in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                EmptyInbox(
                    emptyMessage = mockEmptyMessage,
                    emptyImage = mockEmptyImage
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_BothMessageAndImageDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with custom styles`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            EmptyInbox(
                emptyMessage = mockEmptyMessage,
                emptyMessageStyle = mockCustomTextStyle,
                emptyImage = mockEmptyImage,
                emptyImageStyle = mockCustomImageStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_CustomStyles_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test EmptyInbox with custom styles in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }
        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                EmptyInbox(
                    emptyMessage = mockEmptyMessage,
                    emptyMessageStyle = mockCustomTextStyle,
                    emptyImage = mockEmptyImage,
                    emptyImageStyle = mockCustomImageStyle
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/EmptyInboxTests_CustomStylesDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}
