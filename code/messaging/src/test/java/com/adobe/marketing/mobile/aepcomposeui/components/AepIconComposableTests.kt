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

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepIconStyle
import com.adobe.marketing.mobile.messaging.R
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
class AepIconComposableTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val iconContent = R.drawable.cancel_filled

    private val mockAepIconStyle = AepIconStyle(
        modifier = Modifier.size(50.dp),
        contentDescription = "Cancel",
        tint = Color(0xFF531253)
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Test
    fun `Test AepIconComposable with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.LightGray) {
                AepIconComposable(
                    drawableId = iconContent,
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepIconComposableTests_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepIconComposable with default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepIconComposable(
                    drawableId = iconContent,
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepIconComposableTestsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to AepIconComposable`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepIconComposable(
                    drawableId = iconContent,
                    iconStyle = mockAepIconStyle
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepIconComposableTestsCustomStyle_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to AepIconComposable in dark mode`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.LightGray) {
                TestTheme(useDarkTheme = true) {
                    AepIconComposable(
                        drawableId = iconContent,
                        iconStyle = mockAepIconStyle
                    )
                }
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepIconComposableTestsCustomStyleDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}