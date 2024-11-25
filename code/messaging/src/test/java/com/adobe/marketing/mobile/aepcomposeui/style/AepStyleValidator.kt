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

package com.adobe.marketing.mobile.aepcomposeui.style

import org.junit.Assert.assertEquals

internal class AepStyleValidator {

    companion object {
        /**
         * Validates that the given image style is identical to the expected image style.
         *
         * @param expectedStyle The expected image style.
         * @param style The image style to be validated.
         */
        fun validateImageStyle(expectedStyle: AepImageStyle?, style: AepImageStyle?) {
            assertEquals(expectedStyle?.alpha, style?.alpha)
            assertEquals(expectedStyle?.colorFilter, style?.colorFilter)
            assertEquals(expectedStyle?.contentDescription, style?.contentDescription)
            assertEquals(expectedStyle?.alignment, style?.alignment)
            assertEquals(expectedStyle?.contentScale, style?.contentScale)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        /**
         * Validates that the given text style is identical to the expected text style.
         *
         * @param expectedStyle The expected text style.
         * @param style The text style to be validated.
         */
        fun validateTextStyle(expectedStyle: AepTextStyle?, style: AepTextStyle?) {
            assertEquals(expectedStyle?.textStyle, style?.textStyle)
            assertEquals(expectedStyle?.overflow, style?.overflow)
            assertEquals(expectedStyle?.softWrap, style?.softWrap)
            assertEquals(expectedStyle?.maxLines, style?.maxLines)
            assertEquals(expectedStyle?.minLines, style?.minLines)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        /**
         * Validates that the given row style is identical to the expected row style.
         *
         * @param expectedStyle The expected row style.
         * @param style The row style to be validated.
         */
        fun validateRowStyle(expectedStyle: AepRowStyle?, style: AepRowStyle?) {
            assertEquals(expectedStyle?.verticalAlignment, style?.verticalAlignment)
            assertEquals(expectedStyle?.horizontalArrangement, style?.horizontalArrangement)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        /**
         * Validates that the given button style is identical to the expected button style.
         *
         * @param expectedStyle The expected button style.
         * @param style The button style to be validated.
         */
        fun validateButtonStyle(expectedStyle: AepButtonStyle?, style: AepButtonStyle?) {
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.enabled, style?.enabled)
            assertEquals(expectedStyle?.elevation, style?.elevation)
            assertEquals(expectedStyle?.border, style?.border)
            assertEquals(expectedStyle?.colors, style?.colors)
            assertEquals(expectedStyle?.contentPadding, style?.contentPadding)
            validateTextStyle(expectedStyle?.buttonTextStyle, style?.buttonTextStyle)
        }

        /**
         * Validates that the given icon style is identical to the expected icon style.
         *
         * @param expectedStyle The expected icon style.
         * @param style The icon style to be validated.
         */
        fun validateIconStyle(expectedStyle: AepIconStyle?, style: AepIconStyle?) {
            assertEquals(expectedStyle?.tint, style?.tint)
            assertEquals(expectedStyle?.contentDescription, style?.contentDescription)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        /**
         * Validates that the given card style is identical to the expected card style.
         *
         * @param expectedStyle The expected card style.
         * @param style The card style to be validated.
         */
        fun validateCardStyle(expectedStyle: AepCardStyle?, style: AepCardStyle?) {
            assertEquals(expectedStyle?.border, style?.border)
            assertEquals(expectedStyle?.colors, style?.colors)
            assertEquals(expectedStyle?.elevation, style?.elevation)
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.shape, style?.shape)
        }

        /**
         * Validates that the given column style is identical to the expected column style.
         *
         * @param expectedStyle The expected column style.
         * @param style The column style to be validated.
         */
        fun validateColumnStyle(expectedStyle: AepColumnStyle?, style: AepColumnStyle?) {
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.horizontalAlignment, style?.horizontalAlignment)
            assertEquals(expectedStyle?.verticalArrangement, style?.verticalArrangement)
        }
    }
}
