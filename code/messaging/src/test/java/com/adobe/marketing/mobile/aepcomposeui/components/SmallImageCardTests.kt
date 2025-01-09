/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.components

import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepIconStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.MessagingTestUtils
import com.adobe.marketing.mobile.messaging.R
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheService
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.net.HttpURLConnection
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val mockSmallImageUI
    @Composable
    get() = SmallImageUI(
        template = SmallImageTemplate(
            id = "mockSmallImageCardId",
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
                ),
                AepButton(
                    id = "mockButtonId3",
                    text = AepText(stringResource(id = android.R.string.unknownName)),
                    actionUrl = "mockButtonUrl3"
                )
            ),
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = SmallImageCardUIState()
    )

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class SmallImageCardComposableTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Mock
    private lateinit var mockNetworkService: Networking

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mockBitmap = BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_report_image)

        val simulatedResponse = MessagingTestUtils.simulateNetworkResponse(HttpURLConnection.HTTP_OK, MessagingTestUtils.bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
    }

    @Test
    fun `Test SmallImageCard with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = SmallImageUIStyle.Builder().build(),
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTests_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test SmallImageCard with default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                SmallImageCard(
                    ui = mockSmallImageUI,
                    style = SmallImageUIStyle.Builder().build(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTestsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to enabled SmallImageCard`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = mockCustomSmallImageUIStyle(enabled = true),
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTestsCustomStyleEnabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to disabled SmallImageCard`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = mockCustomSmallImageUIStyle(enabled = false),
                observer = null
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTestsCustomStyleDisabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to enabled SmallImageCard in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = false) {
                SmallImageCard(
                    ui = mockSmallImageUI,
                    style = mockCustomSmallImageUIStyle(enabled = true),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTestsCustomStyleEnabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to disabled SmallImageCard in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = false) {
                SmallImageCard(
                    ui = mockSmallImageUI,
                    style = mockCustomSmallImageUIStyle(enabled = false),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/SmallImageCardTestsCustomStyleDisabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Composable
    private fun mockCustomSmallImageUIStyle(enabled: Boolean): SmallImageUIStyle {
        return SmallImageUIStyle.Builder()
            .cardStyle(
                AepCardStyle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(5.dp),
                    enabled = enabled,
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
            .textColumnStyle(AepColumnStyle(horizontalAlignment = Alignment.CenterHorizontally))
            .titleAepTextStyle(
                AepTextStyle(
                    textStyle = TextStyle(
                        color = Color(0xFFE7CFCD),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Cursive
                    )
                )
            )
            .bodyAepTextStyle(
                AepTextStyle(
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Cursive
                    ),
                    maxLines = 1
                )
            )
            .buttonStyle(
                arrayOf(
                    AepButtonStyle(
                        enabled = enabled,
                        shape = RectangleShape,
                        colors = ButtonColors(
                            containerColor = Color(0xFF456990),
                            contentColor = Color.DarkGray,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray,
                        ),
                        textStyle = AepTextStyle(
                            textStyle = TextStyle(
                                color = Color(0xFFF7ECE1),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Cursive
                            )
                        )
                    ),
                    AepButtonStyle(
                        enabled = enabled,
                        shape = RectangleShape,
                        colors = ButtonColors(
                            containerColor = Color(0xFFa0d2db),
                            contentColor = Color.DarkGray,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray,
                        ),
                        textStyle = AepTextStyle(
                            textStyle = TextStyle(
                                color = Color(0xFF9297c4),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Cursive
                            )
                        )
                    ),
                    AepButtonStyle(
                        enabled = enabled,
                        shape = RectangleShape,
                        colors = ButtonColors(
                            containerColor = Color(0xFFbee7e8),
                            contentColor = Color.DarkGray,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray,
                        ),
                        textStyle = AepTextStyle(
                            textStyle = TextStyle(
                                color = Color(0xFF554640),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Cursive
                            )
                        )
                    )
                )
            )
            .dismissButtonStyle(
                AepIconStyle(
                    modifier = Modifier
                        .size(20.dp),
                    tint = Color(0xFFC6D4FF)
                )
            )
            .dismissButtonAlignment(Alignment.TopStart)
            .build()
    }
}

@RunWith(RobolectricTestRunner::class)
class SmallImageCardBehaviorTests {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Mock
    private lateinit var mockAepUIEventObserver: AepUIEventObserver

    @Mock
    private lateinit var mockCacheService: CacheService
    @Mock
    private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Mock
    private lateinit var mockNetworkService: Networking

    @Before
    fun setUp() {

        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)

        whenever(mockCacheService.set(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )).thenReturn(true)

        // Mocking Cache to bypass cache check
        whenever(
            mockCacheService.get(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).thenReturn(null)

        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)

        MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
        Mockito.validateMockitoUsage()
    }

    @Test
    fun `Test SmallImageCard card click behavior`() {
        // setup
        composeTestRule.setContent {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = SmallImageUIStyle.Builder().build(),
                observer = mockAepUIEventObserver
            )
        }

        // test
        composeTestRule.onRoot().performClick()

        // verify
        val uiEventArgumentCaptor = argumentCaptor<UIEvent<*, *>>()
        verify(mockAepUIEventObserver, times(2)).onEvent(uiEventArgumentCaptor.capture())

        val displayEvent = uiEventArgumentCaptor.allValues[0]
        assertTrue(displayEvent is UIEvent.Display)
        assertTrue(displayEvent.aepUi is SmallImageUI)
        assertEquals("mockSmallImageCardId", (displayEvent.aepUi.getTemplate() as SmallImageTemplate).id)

        val interactEvent = uiEventArgumentCaptor.allValues[1]
        assertTrue(interactEvent is UIEvent.Interact)
        assertTrue(interactEvent.aepUi is SmallImageUI)
        assertTrue(interactEvent.action is UIAction.Click)
        val clickAction = interactEvent.action as UIAction.Click
        assertEquals("Card clicked", clickAction.id)
        assertEquals("mockActionUrl", clickAction.actionUrl)
    }

    @Test
    fun `Test SmallImageCard card button click behavior`() {
        // setup
        composeTestRule.setContent {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = SmallImageUIStyle.Builder().build(),
                observer = mockAepUIEventObserver
            )
        }

        // test
        composeTestRule.onNodeWithText("Cancel").performClick()

        // verify
        val uiEventArgumentCaptor = argumentCaptor<UIEvent<*, *>>()
        verify(mockAepUIEventObserver, times(2)).onEvent(uiEventArgumentCaptor.capture())

        val displayEvent = uiEventArgumentCaptor.allValues[0]
        assertTrue(displayEvent is UIEvent.Display)
        assertTrue(displayEvent.aepUi is SmallImageUI)
        assertEquals("mockSmallImageCardId", (displayEvent.aepUi.getTemplate() as SmallImageTemplate).id)

        val interactEvent = uiEventArgumentCaptor.allValues[1]
        assertTrue(interactEvent is UIEvent.Interact)
        assertTrue(interactEvent.aepUi is SmallImageUI)
        assertTrue(interactEvent.action is UIAction.Click)
        val clickAction = interactEvent.action as UIAction.Click
        assertEquals("mockButtonId2", clickAction.id)
        assertEquals("mockButtonUrl2", clickAction.actionUrl)
    }

    @Test
    fun `Test SmallImageCard card dismiss click behavior`() {
        // setup
        composeTestRule.setContent {
            SmallImageCard(
                ui = mockSmallImageUI,
                style = SmallImageUIStyle.Builder()
                    .dismissButtonStyle(
                        AepIconStyle(
                            modifier = Modifier
                                .size(13.dp)
                                .testTag("dismiss_button")
                        )
                    )
                    .build(),
                observer = mockAepUIEventObserver
            )
        }

        // test
        composeTestRule.onNodeWithTag("dismiss_button").performClick()

        // verify
        val uiEventArgumentCaptor = argumentCaptor<UIEvent<*, *>>()
        verify(mockAepUIEventObserver, times(2)).onEvent(uiEventArgumentCaptor.capture())

        val displayEvent = uiEventArgumentCaptor.allValues[0]
        assertTrue(displayEvent is UIEvent.Display)
        assertTrue(displayEvent.aepUi is SmallImageUI)
        assertEquals("mockSmallImageCardId", (displayEvent.aepUi.getTemplate() as SmallImageTemplate).id)

        val dismissEvent = uiEventArgumentCaptor.allValues[1]
        assertTrue(dismissEvent is UIEvent.Dismiss)
        assertTrue(dismissEvent.aepUi is SmallImageUI)
        assertEquals("mockSmallImageCardId", (dismissEvent.aepUi.getTemplate() as SmallImageTemplate).id)
    }
}
