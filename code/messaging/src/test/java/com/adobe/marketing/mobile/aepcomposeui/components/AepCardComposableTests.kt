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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
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
class AepCardComposableTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val MockAepTextComposable
        @Composable
        get() = AepTextComposable(AepText(stringResource(id = android.R.string.httpErrorBadUrl)))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Test
    fun `Test AepCardComposable with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepCardComposable {
                MockAepTextComposable
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTests_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepCardComposable with default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepCardComposable {
                    MockAepTextComposable
                }
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTestsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style applied to enabled AepCardComposable`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepCardComposable(
                cardStyle = mockCustomAepCardStyle(enabled = true)
            ) {
                MockAepTextComposable
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTestsCustomStyleEnabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style applied to disabled AepCardComposable`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepCardComposable(
                cardStyle = mockCustomAepCardStyle(enabled = false)
            ) {
                MockAepTextComposable
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTestsCustomStyleDisabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style applied to enabled AepCardComposable in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepCardComposable(
                    cardStyle = mockCustomAepCardStyle(enabled = true)
                ) {
                    MockAepTextComposable
                }
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTestsCustomStyleEnabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style applied to disabled AepCardComposable in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepCardComposable(
                    cardStyle = mockCustomAepCardStyle(enabled = false)
                ) {
                    MockAepTextComposable
                }
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepCardComposableTestsCustomStyleDisabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Composable
    private fun mockCustomAepCardStyle(enabled: Boolean): AepCardStyle {
        return AepCardStyle(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(5.dp),
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            colors = CardColors(
                containerColor = Color(0xFF531253),
                contentColor = Color(0xFF8FA998),
                disabledContainerColor = Color.DarkGray,
                disabledContentColor = Color.LightGray,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                disabledElevation = 16.dp
            ),
            border = BorderStroke(2.dp, Color(0xFF33032f))
        )
    }
}
