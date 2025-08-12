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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepButtonStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
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
class AepButtonRowComposableTests(
    private val qualifier: String
) {
    @get: Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockAepButton1
        @Composable
        get() = AepButton(
            id = "mockId1",
            actionUrl = "mockActionUrl1",
            text = AepText(stringResource(id = android.R.string.ok)),
        )

    private val mockAepButton2
        @Composable
        get() = AepButton(
            id = "mockId2",
            actionUrl = "mockActionUrl2",
            text = AepText(stringResource(id = android.R.string.cancel)),
        )

    private val mockAepButton3
        @Composable
        get() = AepButton(
            id = "mockId3",
            actionUrl = "mockActionUrl3",
            text = AepText(stringResource(id = android.R.string.unknownName)),
        )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun qualifiersProvider(): Array<String> {
            return SnapshotTestQualifierProvider.qualifiersProvider()
        }
    }

    @Test
    fun `Test AepButtonRowComposable with three buttons and default style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            AepButtonRowComposable(
                buttons = listOf(mockAepButton1, mockAepButton2, mockAepButton3),
                buttonsStyle = arrayOf(AepButtonStyle(), AepButtonStyle(), AepButtonStyle()),
                onClick = { }
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonRowComposableTestsDefault_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepButtonRowComposable with three buttons and default style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepButtonRowComposable(
                    buttons = listOf(mockAepButton1, mockAepButton2, mockAepButton3),
                    buttonsStyle = arrayOf(AepButtonStyle(), AepButtonStyle(), AepButtonStyle()),
                    onClick = { }
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonRowComposableTestsDefaultDark_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepButtonRowComposable with three buttons and custom style`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            AepButtonRowComposable(
                buttons = listOf(mockAepButton1, mockAepButton2, mockAepButton3),
                buttonsStyle = arrayOf(mockCustomAepButtonStyle(), mockCustomAepButtonStyle(), mockCustomAepButtonStyle()),
                rowStyle = AepRowStyle(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ),
                onClick = { }
            )
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonRowComposableTestsCustom_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Test
    fun `Test AepButtonRowComposable with three buttons and custom style in dark theme`() {
        // setup
        RuntimeEnvironment.setQualifiers(qualifier)

        // test
        setComposeContent(
            composeTestRule,
            qualifier
        ) {
            TestTheme(useDarkTheme = true) {
                AepButtonRowComposable(
                    buttons = listOf(mockAepButton1, mockAepButton2, mockAepButton3),
                    buttonsStyle = arrayOf(
                        mockCustomAepButtonStyle(),
                        mockCustomAepButtonStyle(),
                        mockCustomAepButtonStyle()
                    ),
                    rowStyle = AepRowStyle(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ),
                    onClick = { }
                )
            }
        }

        // Capture screenshot
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "build/outputs/roborazzi/AepButtonRowComposableTestsCustomDark_${Build.VERSION.SDK_INT}_$qualifier.png")
    }

    @Composable
    private fun mockCustomAepButtonStyle(): AepButtonStyle {
        return AepButtonStyle(
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
        override val interactions: Flow<Interaction> = emptyFlow()
        override suspend fun emit(interaction: Interaction) {}
        override fun tryEmit(interaction: Interaction): Boolean = false
    }
}
