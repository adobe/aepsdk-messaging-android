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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.aepcomposeui.style.AepIconStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.messaging.R
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
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
class AepDismissButtonTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val iconContent = R.drawable.close_filled
    private val mockAepIcon
        get() = AepIcon(iconContent)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Test
    fun `Test AepDismissButton with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            AepDismissButton(
                modifier = Modifier,
                dismissIcon = mockAepIcon,
                onClick = {}
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepDismissButtonTests_Default_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepDismissButton with default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepDismissButton(
                    modifier = Modifier,
                    dismissIcon = mockAepIcon,
                    onClick = {}
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepDismissButtonTests_DefaultDark_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepDismissButton with custom style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)
        val customStyle = AepIconStyle(
            tint = Color.Red,
            contentDescription = "Custom description"
        )

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            AepDismissButton(
                modifier = Modifier,
                dismissIcon = mockAepIcon,
                style = customStyle,
                onClick = {}
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepDismissButtonTests_Custom_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepDismissButton with custom style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)
        val customStyle = AepIconStyle(
            tint = Color.Green,
            contentDescription = "Custom description dark"
        )

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepDismissButton(
                    modifier = Modifier,
                    dismissIcon = mockAepIcon,
                    style = customStyle,
                    onClick = {}
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepDismissButtonTests_CustomDark_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepDismissButton onClick is called`() {
        // setup
        var clicked by mutableStateOf(false)

        // test
        composeTestRule.setContent {
            AepDismissButton(
                modifier = Modifier.testTag("test"),
                dismissIcon = mockAepIcon,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithTag("test").performClick()

        // verify
        assertTrue(clicked)
    }
}
