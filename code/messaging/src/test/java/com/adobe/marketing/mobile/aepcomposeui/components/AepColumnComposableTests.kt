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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
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
class AepColumnComposableTests(
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
    fun `Test AepColumnComposable with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepColumnComposable {
                MockAepTextComposable
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepColumnComposableTests_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style applied to AepColumnComposable`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepColumnComposable(
                columnStyle = AepColumnStyle(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(500.dp)
                        .background(Color(0xFF0065db))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                )
            ) {
                MockAepTextComposable
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepColumnComposableTestsCustomStyle_${Build.VERSION.SDK_INT}_$qualifier.png")
    }
}
