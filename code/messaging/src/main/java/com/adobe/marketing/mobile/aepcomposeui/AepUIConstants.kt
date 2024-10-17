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

package com.adobe.marketing.mobile.aepcomposeui

import androidx.compose.ui.text.font.FontWeight

internal class AepUIConstants {

    internal class DefaultStyle {
        companion object {
            const val TITLE_TEXT_SIZE = 15
            val TITLE_FONT_WEIGHT = FontWeight.Medium
            const val BODY_TEXT_SIZE = 13
            val BODY_FONT_WEIGHT = FontWeight.Normal
            const val BUTTON_TEXT_SIZE = 13
            val BUTTON_FONT_WEIGHT = FontWeight.Normal
        }
    }
}