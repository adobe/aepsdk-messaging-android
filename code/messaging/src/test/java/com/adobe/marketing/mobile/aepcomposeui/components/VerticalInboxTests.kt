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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardImageManager
import com.adobe.marketing.mobile.messaging.R
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
@Config(sdk = [33])
class VerticalInboxTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

    @Composable
    private fun mockSmallImageUI(id: String, dismissed: Boolean = false, read: Boolean? = null) = SmallImageUI(
        template = SmallImageTemplate(
            id = id,
            title = AepText(stringResource(id = android.R.string.dialog_alert_title)),
            body = AepText(stringResource(id = android.R.string.httpErrorBadUrl)),
            image = AepImage("https://www.mockImageUrl.com"),
            actionUrl = "mockActionUrl",
            buttons = listOf(
                AepButton(
                    id = "mockButtonId1",
                    text = AepText(stringResource(id = android.R.string.ok)),
                    actionUrl = "mockButtonUrl1"
                ),
                AepButton(
                    id = "mockButtonId2",
                    text = AepText(stringResource(id = android.R.string.cancel)),
                    actionUrl = "mockButtonUrl2"
                )
            ),
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = SmallImageCardUIState(dismissed = dismissed, read = read)
    )

    @Composable
    private fun mockLargeImageUI(id: String, dismissed: Boolean = false, read: Boolean? = null) = LargeImageUI(
        template = LargeImageTemplate(
            id = id,
            title = AepText(stringResource(id = android.R.string.dialog_alert_title)),
            body = AepText(stringResource(id = android.R.string.httpErrorBadUrl)),
            image = AepImage("https://www.mockImageUrl.com"),
            actionUrl = "mockActionUrl",
            buttons = listOf(
                AepButton(
                    id = "mockButtonId1",
                    text = AepText(stringResource(id = android.R.string.ok)),
                    actionUrl = "mockButtonUrl1"
                )
            ),
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = LargeImageCardUIState(dismissed = dismissed, read = read)
    )

    @Composable
    private fun mockImageOnlyUI(id: String, dismissed: Boolean = false, read: Boolean? = null) = ImageOnlyUI(
        template = ImageOnlyTemplate(
            id = id,
            image = AepImage("https://www.mockImageUrl.com"),
            actionUrl = "mockActionUrl",
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = ImageOnlyCardUIState(dismissed = dismissed, read = read)
    )

    private fun createInboxTemplate(
        emptyMessage: AepText? = AepText("No messages"),
        emptyImage: AepImage? = null
    ) = InboxTemplate(
        heading = AepText("Inbox"),
        layout = AepInboxLayout.VERTICAL,
        capacity = 10,
        emptyMessage = emptyMessage,
        emptyImage = emptyImage
    )

    private fun setupImageMocking(bitmap: Bitmap) {
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(bitmap))
        }
    }

    @Test
    fun `Test VerticalInbox with mixed items`() {
        // setup
        setupImageMocking(mockLightBitmap)

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createInboxTemplate()
            val items = listOf(
                mockSmallImageUI("item1"),
                mockLargeImageUI("item2"),
                mockImageOnlyUI("item3")
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            VerticalInbox(
                ui = uiState,
                inboxStyle = InboxUIStyle.Builder().build(),
                readCardsStyle = AepUIStyle(),
                unreadCardsStyle = AepUIStyle(),
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/VerticalInboxTests_MixedItems_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test VerticalInbox with mixed items in dark theme`() {
        // setup
        setupImageMocking(mockDarkBitmap)

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createInboxTemplate()
                val items = listOf(
                    mockSmallImageUI("item1"),
                    mockLargeImageUI("item2"),
                    mockImageOnlyUI("item3")
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                VerticalInbox(
                    ui = uiState,
                    inboxStyle = InboxUIStyle.Builder().build(),
                    readCardsStyle = AepUIStyle(),
                    unreadCardsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/VerticalInboxTests_MixedItemsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test VerticalInbox respects capacity limit`() {
        // setup
        setupImageMocking(mockLightBitmap)

        // test
        setComposeContent(composeTestRule, qualifier) {
            // Template with capacity of 2
            val template = InboxTemplate(
                heading = AepText("Inbox"),
                layout = AepInboxLayout.VERTICAL,
                capacity = 2,
                emptyMessage = AepText("No messages")
            )
            val items = listOf(
                mockSmallImageUI("item1"),
                mockLargeImageUI("item2"),
                mockImageOnlyUI("item3"), // Should not be shown
                mockSmallImageUI("item4") // Should not be shown
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            VerticalInbox(
                ui = uiState,
                inboxStyle = InboxUIStyle.Builder().build(),
                readCardsStyle = AepUIStyle(),
                unreadCardsStyle = AepUIStyle(),
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/VerticalInboxTests_CapacityLimit_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Composable
    private fun mockCustomCardsStyle(): AepUIStyle {
        return AepUIStyle(
            smallImageUIStyle = SmallImageUIStyle.Builder()
                .cardStyle(
                    AepCardStyle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(5.dp),
                        enabled = true,
                        shape = RectangleShape,
                        colors = CardColors(
                            containerColor = Color(0xFF531253),
                            contentColor = Color(0xFFF7ECE1),
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.LightGray,
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                            disabledElevation = 16.dp
                        ),
                        border = BorderStroke(2.dp, Color(0xFF33032f))
                    )
                )
                .rootRowStyle(
                    AepRowStyle(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    )
                )
                .imageStyle(
                    AepImageStyle(
                        modifier = Modifier
                            .size(50.dp)
                            .aspectRatio(1.5f),
                        contentDescription = "Test Image",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.FillBounds
                    )
                )
                .build(),
            largeImageUIStyle = LargeImageUIStyle.Builder()
                .cardStyle(
                    AepCardStyle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(5.dp),
                        enabled = true,
                        shape = RectangleShape,
                        colors = CardColors(
                            containerColor = Color(0xFF1E3A5F),
                            contentColor = Color(0xFFFFFFFF),
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.LightGray,
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                            disabledElevation = 16.dp
                        ),
                        border = BorderStroke(2.dp, Color(0xFF0D1F2D))
                    )
                )
                .rootColumnStyle(
                    AepColumnStyle(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    )
                )
                .imageStyle(
                    AepImageStyle(
                        modifier = Modifier
                            .size(90.dp)
                            .aspectRatio(1.5f),
                        contentDescription = "Test Image",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.FillBounds
                    )
                )
                .build(),
            imageOnlyUIStyle = ImageOnlyUIStyle.Builder()
                .cardStyle(
                    AepCardStyle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(5.dp),
                        enabled = true,
                        shape = RectangleShape,
                        colors = CardColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color(0xFFFFFFFF),
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.LightGray,
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp,
                            disabledElevation = 12.dp
                        ),
                        border = BorderStroke(2.dp, Color(0xFF1B5E20))
                    )
                )
                .imageStyle(
                    AepImageStyle(
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.Center,
                        contentScale = ContentScale.FillBounds,
                        contentDescription = "Custom Image"
                    )
                )
                .build()
        )
    }

    @Test
    fun `Test VerticalInbox with custom styles`() {
        // setup
        setupImageMocking(mockLightBitmap)

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createInboxTemplate()
            val items = listOf(
                mockSmallImageUI("item1"),
                mockLargeImageUI("item2"),
                mockImageOnlyUI("item3")
            )
            val uiState = InboxUIState.Success(template = template, items = items)
            val customStyle = mockCustomCardsStyle()

            VerticalInbox(
                ui = uiState,
                inboxStyle = InboxUIStyle.Builder().build(),
                readCardsStyle = customStyle,
                unreadCardsStyle = customStyle,
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/VerticalInboxTests_CustomStyles_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test VerticalInbox with custom styles in dark theme`() {
        // setup
        setupImageMocking(mockDarkBitmap)

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createInboxTemplate()
                val items = listOf(
                    mockSmallImageUI("item1"),
                    mockLargeImageUI("item2"),
                    mockImageOnlyUI("item3")
                )
                val uiState = InboxUIState.Success(template = template, items = items)
                val customStyle = mockCustomCardsStyle()

                VerticalInbox(
                    ui = uiState,
                    inboxStyle = InboxUIStyle.Builder().build(),
                    readCardsStyle = customStyle,
                    unreadCardsStyle = customStyle,
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/VerticalInboxTests_CustomStylesDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}
