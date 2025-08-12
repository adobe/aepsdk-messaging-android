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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.example.compose.TestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class AepButtonTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockAepText
        @Composable
        @ReadOnlyComposable
        get() = AepText(stringResource(id = android.R.string.httpErrorBadUrl))

    private val mockAepButton
        @Composable
        @ReadOnlyComposable
        get() = AepButton(
            id = "mockId",
            actionUrl = "mockActionUrl",
            text = mockAepText,
        )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Test
    fun `Test AepButton with default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepButton(
                model = mockAepButton,
                onClick = { }
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTests_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepButton with default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepButton(
                    model = mockAepButton,
                    onClick = { }
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTestsDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to enabled AepButton`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepButton(
                model = mockAepButton,
                onClick = {},
                buttonStyle = mockCustomAepButtonStyle(true)
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTestsCustomStyleEnabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to disabled AepButton`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            AepButton(
                model = mockAepButton,
                onClick = { },
                buttonStyle = mockCustomAepButtonStyle(false)
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTestsCustomStyleDisabled_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to enabled AepButton in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepButton(
                    model = mockAepButton,
                    onClick = { },
                    buttonStyle = mockCustomAepButtonStyle(true)
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTestsCustomStyleEnabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test custom style is applied to disabled AepButton in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule, qualifier
        ) {
            TestTheme(useDarkTheme = false) {
                AepButton(
                    model = mockAepButton,
                    onClick = { },
                    buttonStyle = mockCustomAepButtonStyle(false)
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonTestsCustomStyleDisabledDarkTheme_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Composable
    private fun mockCustomAepButtonStyle(enabled: Boolean): AepButtonStyle {
        return AepButtonStyle(
            modifier = Modifier.height(50.dp).width(200.dp),
            enabled = enabled,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 10.dp,
                focusedElevation = 10.dp,
                hoveredElevation = 10.dp,
                disabledElevation = 10.dp
            ),
            shape = CutCornerShape(10.dp),
            border = BorderStroke(2.dp, Color(0xFF004643)),
            colors = ButtonColors(
                containerColor = Color(0xFF0065db),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF33032f),
                disabledContentColor = Color.Gray,
            ),
            contentPadding = PaddingValues(10.dp),
            interactionSource = NoEffectInteractionSource(),
        )
    }

    class NoEffectInteractionSource : MutableInteractionSource {
        override val interactions: Flow<Interaction> = emptyFlow() // No interactions emitted
        override suspend fun emit(interaction: Interaction) {
            // Do nothing
        }

        override fun tryEmit(interaction: Interaction): Boolean {
            return false
        }
    }
}
