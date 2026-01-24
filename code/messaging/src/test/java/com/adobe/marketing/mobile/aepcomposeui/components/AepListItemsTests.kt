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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardImageManager
import com.adobe.marketing.mobile.messaging.R
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
class AepListItemsTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
                ),
                AepButton(
                    id = "mockButtonId3",
                    text = AepText(stringResource(id = android.R.string.unknownName)),
                    actionUrl = "mockButtonUrl3"
                )
            ),
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = SmallImageCardUIState(dismissed = dismissed, read = read)
    )

    @Composable
    private fun mockLargeImageUI(id: String) = LargeImageUI(
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
        state = LargeImageCardUIState()
    )

    @Composable
    private fun mockImageOnlyUI(id: String) = ImageOnlyUI(
        template = ImageOnlyTemplate(
            id = id,
            image = AepImage("https://www.mockImageUrl.com"),
            actionUrl = "mockActionUrl",
            dismissBtn = AepIcon(R.drawable.close_filled)
        ),
        state = ImageOnlyCardUIState()
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    private lateinit var mockLightBitmap: Bitmap
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
    fun `Test renderListItems with SmallImageUI items`() {
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
            val items = listOf(mockSmallImageUI("1"), mockSmallImageUI("2"), mockSmallImageUI("3"))

            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_SmallImage_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with LargeImageUI items`() {
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
            val items = listOf(mockLargeImageUI("1"), mockLargeImageUI("2"), mockLargeImageUI("3"))
            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_LargeImage_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with ImageOnlyUI items`() {
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
            val items = listOf(mockImageOnlyUI("1"), mockImageOnlyUI("2"), mockImageOnlyUI("3"))
            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_LargeImage_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with mixed items`() {
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
            val items = listOf(mockSmallImageUI("1"), mockLargeImageUI("2"), mockImageOnlyUI("3"))
            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_Mixed_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with dismissed item`() {
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
            val items = listOf(mockSmallImageUI("1", dismissed = true), mockLargeImageUI("2"), mockImageOnlyUI("3"))
            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null
                )
            }
        }

        // Capture screenshot - should only show 2 cards (dismissed one is hidden)
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_Dismissed_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with mixed read and unread items`() {
        // setup
        mockkObject(ContentCardImageManager)
        every {
            ContentCardImageManager.getContentCardImageBitmap(any(), any(), any())
        } answers {
            thirdArg<(Result<Bitmap>) -> Unit>().invoke(Result.success(mockLightBitmap))
        }

        // setup
        val unreadIcon = AepImage("https://www.mockUnreadIconUrl.com")
        val unreadIconStyle = AepImageStyle()
        val unreadIconAlignment = Alignment.TopEnd

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            val items = listOf(
                mockSmallImageUI("1", read = false), // unread - should show icon
                mockLargeImageUI("2"), // read - no icon
                mockImageOnlyUI("3") // read - no icon
            )

            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    observer = null,
                    unreadIcon = Triple(unreadIcon, unreadIconStyle, unreadIconAlignment)
                )
            }
        }

        // Capture screenshot - only first item should show unread icon
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_UnreadIcon_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test renderListItems with unreadItemsStyle set`() {
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
            val items = listOf(
                mockSmallImageUI("1", read = false), // unread - should use unreadItemsStyle
                mockLargeImageUI("2"), // read - should use default itemsStyle
                mockImageOnlyUI("3") // read - should use default itemsStyle
            )

            // Create a distinct style for unread items with a grey background
            val unreadCardStyle = AepCardStyle(
                colors = CardDefaults.cardColors(Color.LightGray)
            )
            val unreadSmallImageStyle = SmallImageUIStyle.Builder()
                .cardStyle(unreadCardStyle)
                .build()
            val unreadItemsStyle = AepUIStyle(
                smallImageUIStyle = unreadSmallImageStyle
            )

            LazyColumn {
                renderListItems(
                    items = items,
                    readItemsStyle = AepUIStyle(),
                    unreadItemsStyle = unreadItemsStyle,
                    observer = null,
                )
            }
        }

        // Capture screenshot - first item should have gray border (unread style), second and third should not
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepListItemsTests_UnreadBackground_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}
