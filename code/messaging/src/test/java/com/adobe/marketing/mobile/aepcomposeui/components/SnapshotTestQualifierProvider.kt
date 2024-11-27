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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.LayoutDirection
import org.robolectric.ParameterizedRobolectricTestRunner

object SnapshotTestQualifierProvider {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters
    fun qualifiersProvider(): Array<String> =
        arrayOf(
            "en-rUS", // LTR (English, US)
            "ar-rSA", // RTL (Arabic, Saudi Arabia)
            "small", // Screen size small
            "normal", // Screen size normal
            "large", // Screen size large
            "xlarge", // Screen size xlarge
            "land", // Landscape
            "port" // Portrait
        )
}

// Custom settings for all compose tests
fun setComposeContent(composeTestRule: ComposeContentTestRule, qualifier: String, content: @Composable () -> Unit) {
    composeTestRule.setContent {
        // Set the layout direction based on the qualifier
        if (qualifier.contains("ar-rSA")) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        } else {
            content()
        }
    }
}
