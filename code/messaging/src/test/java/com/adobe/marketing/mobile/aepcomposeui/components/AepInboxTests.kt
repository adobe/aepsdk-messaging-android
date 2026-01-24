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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
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
class AepInboxTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockEmptyMessage
        @Composable
        @ReadOnlyComposable
        get() = AepText(stringResource(android.R.string.httpErrorBadUrl))
    private val mockImageUrl = "https://example.com/empty_inbox.png"
    private val mockDarkImageUrl = "https://example.com/empty_inbox_dark.png"
    private val mockEmptyImage = AepImage(url = mockImageUrl, darkUrl = mockDarkImageUrl)

    private lateinit var mockLightBitmap: Bitmap
    private lateinit var mockDarkBitmap: Bitmap

    private val mockCustomEmptyMessageStyle = AepTextStyle(
        modifier = Modifier.padding(16.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFF0065db),
            fontSize = 18.sp
        )
    )

    private val mockCustomEmptyImageStyle = AepImageStyle(
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
                )
            ),
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = SmallImageCardUIState(dismissed = dismissed, read = read)
    )

    @Composable
    private fun createEmptyInboxTemplate(
        emptyMessage: AepText? = mockEmptyMessage,
        emptyImage: AepImage? = null
    ) = InboxTemplate(
        heading = AepText("Inbox"),
        layout = AepInboxLayout.VERTICAL,
        capacity = 10,
        emptyMessage = emptyMessage,
        emptyImage = emptyImage
    )

    @Composable
    private fun createInboxTemplateWithItems(
        unreadBgColor: AepColor? = null,
        unreadIcon: AepImage? = null,
        unreadIconAlignment: Alignment? = null
    ) = InboxTemplate(
        heading = AepText("Inbox"),
        layout = AepInboxLayout.VERTICAL,
        capacity = 10,
        emptyMessage = AepText("No messages"),
        isUnreadEnabled = unreadBgColor != null || unreadIcon != null,
        unreadBgColor = unreadBgColor,
        unreadIcon = unreadIcon,
        unreadIconAlignment = unreadIconAlignment
    )

    @Composable
    private fun createHorizontalInboxTemplate(
        emptyMessage: AepText? = mockEmptyMessage,
        emptyImage: AepImage? = null
    ) = InboxTemplate(
        heading = AepText("Horizontal Inbox"),
        layout = AepInboxLayout.HORIZONTAL,
        capacity = 10,
        emptyMessage = emptyMessage,
        emptyImage = emptyImage
    )

    @Composable
    private fun createHorizontalInboxTemplateWithItems(
        unreadBgColor: AepColor? = null,
        unreadIcon: AepImage? = null,
        unreadIconAlignment: Alignment? = null
    ) = InboxTemplate(
        heading = AepText("Horizontal Inbox"),
        layout = AepInboxLayout.HORIZONTAL,
        capacity = 10,
        emptyMessage = AepText("No messages"),
        isUnreadEnabled = unreadBgColor != null || unreadIcon != null,
        unreadBgColor = unreadBgColor,
        unreadIcon = unreadIcon,
        unreadIconAlignment = unreadIconAlignment
    )

    @Test
    fun `Test AepInbox with loading state`() {
        // setup
        val uiState = InboxUIState.Loading

        // test
        setComposeContent(composeTestRule, qualifier) {
            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_LoadingState_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with loading state with custom loading view`() {
        // setup
        val uiState = InboxUIState.Loading
        val customStyle = InboxUIStyle.Builder()
            .loadingView {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Loading your messages...",
                        color = Color(0xFF1976D2),
                        fontSize = 16.sp
                    )
                }
            }
            .build()

        // test
        setComposeContent(composeTestRule, qualifier) {
            AepInbox(
                uiState = uiState,
                inboxStyle = customStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_LoadingStateCustomView_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createEmptyInboxTemplate()
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyState_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createEmptyInboxTemplate()
                val uiState = InboxUIState.Success(template = template, items = emptyList())
                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyStateDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state with image`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createEmptyInboxTemplate(emptyImage = mockEmptyImage)
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyStateWithImage_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state with image in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createEmptyInboxTemplate(emptyImage = mockEmptyImage)
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            TestTheme(useDarkTheme = true) {
                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyStateWithImageDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state with custom styles`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createEmptyInboxTemplate(emptyImage = mockEmptyImage)
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            val customStyle = InboxUIStyle.Builder()
                .emptyMessageStyle(mockCustomEmptyMessageStyle)
                .emptyImageStyle(mockCustomEmptyImageStyle)
                .build()

            AepInbox(
                uiState = uiState,
                inboxStyle = customStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyStateCustomStyles_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with empty state with custom styles in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createEmptyInboxTemplate(emptyImage = mockEmptyImage)
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            val customStyle = InboxUIStyle.Builder()
                .emptyMessageStyle(mockCustomEmptyMessageStyle)
                .emptyImageStyle(mockCustomEmptyImageStyle)
                .build()
            TestTheme(useDarkTheme = true) {
                AepInbox(
                    uiState = uiState,
                    inboxStyle = customStyle
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_EmptyStateCustomStylesDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with error state`() {
        // setup
        val uiState = InboxUIState.Error(Throwable("Failed to load inbox"))

        // test
        setComposeContent(composeTestRule, qualifier) {
            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_ErrorState_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with error state with custom error view`() {
        // setup
        val uiState = InboxUIState.Error(Throwable("Failed to load inbox"))
        val customStyle = InboxUIStyle.Builder()
            .errorView {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Oops! Something went wrong",
                        color = Color(0xFFD32F2F),
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Please try again later",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            .build()

        // test
        setComposeContent(composeTestRule, qualifier) {
            AepInbox(
                uiState = uiState,
                inboxStyle = customStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_ErrorStateCustomView_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with unread items and unread background color`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createInboxTemplateWithItems(
                unreadBgColor = AepColor(
                    light = Color(0xFFE3F2FD),
                    dark = Color(0xFF1A237E)
                )
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread
                mockSmallImageUI("item2", read = true), // read
                mockSmallImageUI("item3", read = false) // unread
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_UnreadBgColor_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with unread items and unread background color in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createInboxTemplateWithItems(
                    unreadBgColor = AepColor(
                        light = Color(0xFFE3F2FD),
                        dark = Color(0xFF1A237E)
                    )
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread
                    mockSmallImageUI("item2", read = true), // read
                    mockSmallImageUI("item3", read = false) // unread
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_UnreadBgColorDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with unread icon`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createInboxTemplateWithItems(
                unreadIcon = AepImage(
                    url = "https://example.com/unread_icon.png"
                ),
                unreadIconAlignment = Alignment.TopEnd
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread - shows icon
                mockSmallImageUI("item2", read = true), // read - no icon
                mockSmallImageUI("item3", read = false) // unread - shows icon
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_UnreadIcon_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with unread icon in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createInboxTemplateWithItems(
                    unreadIcon = AepImage(
                        url = "https://example.com/unread_icon.png"
                    ),
                    unreadIconAlignment = Alignment.TopEnd
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread - shows icon
                    mockSmallImageUI("item2", read = true), // read - no icon
                    mockSmallImageUI("item3", read = false) // unread - shows icon
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_UnreadIconDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with custom unreadBgColor from InboxUIStyle`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            // Template has unread enabled but no unreadBgColor
            val template = InboxTemplate(
                heading = AepText("Inbox"),
                layout = AepInboxLayout.VERTICAL,
                capacity = 10,
                emptyMessage = AepText("No messages"),
                isUnreadEnabled = true
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread
                mockSmallImageUI("item2", read = true), // read
                mockSmallImageUI("item3", read = false) // unread
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            // Override unreadBgColor via InboxUIStyle
            val customStyle = InboxUIStyle.Builder()
                .unreadBgColor(
                    AepColor(
                        light = Color(0xFFFFF3E0), // Light orange
                        dark = Color(0xFFE65100) // Dark orange
                    )
                )
                .build()

            AepInbox(
                uiState = uiState,
                inboxStyle = customStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_CustomUnreadBgColorFromStyle_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with custom unreadBgColor from InboxUIStyle in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // Set night mode configuration for isSystemInDarkTheme() to return true
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cfg = ctx.resources.configuration
        cfg.uiMode = Configuration.UI_MODE_NIGHT_YES or
            (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)
        ctx.resources.updateConfiguration(cfg, ctx.resources.displayMetrics)

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                // Template has unread enabled but no unreadBgColor
                val template = InboxTemplate(
                    heading = AepText("Inbox"),
                    layout = AepInboxLayout.VERTICAL,
                    capacity = 10,
                    emptyMessage = AepText("No messages"),
                    isUnreadEnabled = true
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread
                    mockSmallImageUI("item2", read = true), // read
                    mockSmallImageUI("item3", read = false) // unread
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                // Override unreadBgColor via InboxUIStyle
                val customStyle = InboxUIStyle.Builder()
                    .unreadBgColor(
                        AepColor(
                            light = Color(0xFFFFF3E0), // Light orange
                            dark = Color(0xFFE65100) // Dark orange
                        )
                    )
                    .build()

                AepInbox(
                    uiState = uiState,
                    inboxStyle = customStyle
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_CustomUnreadBgColorFromStyleDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with custom unreadIconStyle from InboxUIStyle`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            // Template has unread icon
            val template = InboxTemplate(
                heading = AepText("Inbox"),
                layout = AepInboxLayout.VERTICAL,
                capacity = 10,
                emptyMessage = AepText("No messages"),
                isUnreadEnabled = true,
                unreadIcon = AepImage(url = "https://example.com/unread_icon.png"),
                unreadIconAlignment = Alignment.TopStart
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread - shows icon
                mockSmallImageUI("item2", read = true), // read - no icon
                mockSmallImageUI("item3", read = false) // unread - shows icon
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            // Override unreadIconStyle via InboxUIStyle with custom size and padding
            val customStyle = InboxUIStyle.Builder()
                .unreadIconStyle(
                    AepImageStyle(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp),
                        contentDescription = "Custom unread indicator"
                    )
                )
                .unreadIconAlignment(Alignment.TopEnd)
                .build()

            AepInbox(
                uiState = uiState,
                inboxStyle = customStyle
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_CustomUnreadIconStyleFromStyle_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepInbox with custom unreadIconStyle from InboxUIStyle in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // Set night mode configuration for isSystemInDarkTheme() to return true
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cfg = ctx.resources.configuration
        cfg.uiMode = Configuration.UI_MODE_NIGHT_YES or
            (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)
        ctx.resources.updateConfiguration(cfg, ctx.resources.displayMetrics)

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                // Template has unread icon
                val template = InboxTemplate(
                    heading = AepText("Inbox"),
                    layout = AepInboxLayout.VERTICAL,
                    capacity = 10,
                    emptyMessage = AepText("No messages"),
                    isUnreadEnabled = true,
                    unreadIcon = AepImage(url = "https://example.com/unread_icon.png"),
                    unreadIconAlignment = Alignment.TopStart
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread - shows icon
                    mockSmallImageUI("item2", read = true), // read - no icon
                    mockSmallImageUI("item3", read = false) // unread - shows icon
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                // Override unreadIconStyle via InboxUIStyle with custom size and padding
                val customStyle = InboxUIStyle.Builder()
                    .unreadIconStyle(
                        AepImageStyle(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(32.dp),
                            contentDescription = "Custom unread indicator"
                        )
                    )
                    .unreadIconAlignment(Alignment.TopEnd)
                    .build()

                AepInbox(
                    uiState = uiState,
                    inboxStyle = customStyle
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_CustomUnreadIconStyleFromStyleDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with items`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createHorizontalInboxTemplateWithItems()
            val items = listOf(
                mockSmallImageUI("item1"),
                mockSmallImageUI("item2"),
                mockSmallImageUI("item3")
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalWithItems_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with items in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createHorizontalInboxTemplateWithItems()
                val items = listOf(
                    mockSmallImageUI("item1"),
                    mockSmallImageUI("item2"),
                    mockSmallImageUI("item3")
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalWithItemsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with empty state`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createHorizontalInboxTemplate()
            val uiState = InboxUIState.Success(template = template, items = emptyList())
            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalEmptyState_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with unread items and unread background color`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createHorizontalInboxTemplateWithItems(
                unreadBgColor = AepColor(
                    light = Color(0xFFE3F2FD),
                    dark = Color(0xFF1A237E)
                )
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread
                mockSmallImageUI("item2", read = true), // read
                mockSmallImageUI("item3", read = false) // unread
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalUnreadBgColor_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with unread items and unread background color in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createHorizontalInboxTemplateWithItems(
                    unreadBgColor = AepColor(
                        light = Color(0xFFE3F2FD),
                        dark = Color(0xFF1A237E)
                    )
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread
                    mockSmallImageUI("item2", read = true), // read
                    mockSmallImageUI("item3", read = false) // unread
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalUnreadBgColorDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with unread icon`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            val template = createHorizontalInboxTemplateWithItems(
                unreadIcon = AepImage(
                    url = "https://example.com/unread_icon.png"
                ),
                unreadIconAlignment = Alignment.TopEnd
            )
            val items = listOf(
                mockSmallImageUI("item1", read = false), // unread - shows icon
                mockSmallImageUI("item2", read = true), // read - no icon
                mockSmallImageUI("item3", read = false) // unread - shows icon
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalUnreadIcon_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox with unread icon in dark theme`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockDarkBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            TestTheme(useDarkTheme = true) {
                val template = createHorizontalInboxTemplateWithItems(
                    unreadIcon = AepImage(
                        url = "https://example.com/unread_icon.png"
                    ),
                    unreadIconAlignment = Alignment.TopEnd
                )
                val items = listOf(
                    mockSmallImageUI("item1", read = false), // unread - shows icon
                    mockSmallImageUI("item2", read = true), // read - no icon
                    mockSmallImageUI("item3", read = false) // unread - shows icon
                )
                val uiState = InboxUIState.Success(template = template, items = items)

                AepInbox(
                    uiState = uiState,
                    inboxStyle = InboxUIStyle.Builder().build()
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalUnreadIconDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test HorizontalInbox respects capacity limit`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // test
        setComposeContent(composeTestRule, qualifier) {
            // Create template with capacity of 2
            val template = InboxTemplate(
                heading = AepText("Limited Horizontal Inbox"),
                layout = AepInboxLayout.HORIZONTAL,
                capacity = 2,
                emptyMessage = AepText("No messages")
            )
            // Provide more items than capacity
            val items = listOf(
                mockSmallImageUI("item1"),
                mockSmallImageUI("item2"),
                mockSmallImageUI("item3"),
                mockSmallImageUI("item4")
            )
            val uiState = InboxUIState.Success(template = template, items = items)

            AepInbox(
                uiState = uiState,
                inboxStyle = InboxUIStyle.Builder().build()
            )
        }

        // Capture screenshot - should only show 2 items
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepInboxTests_HorizontalCapacityLimit_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}
