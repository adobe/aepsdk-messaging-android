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

class AepStyleValidator {

    companion object {
        fun validateImageStyle(expectedStyle: AepImageStyle?, style: AepImageStyle?) {
            assertEquals(expectedStyle?.alpha, style?.alpha)
            assertEquals(expectedStyle?.colorFilter, style?.colorFilter)
            assertEquals(expectedStyle?.contentDescription, style?.contentDescription)
            assertEquals(expectedStyle?.alignment, style?.alignment)
            assertEquals(expectedStyle?.contentScale, style?.contentScale)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        fun validateTextStyle(expectedStyle: AepTextStyle?, style: AepTextStyle?) {
            assertEquals(expectedStyle?.textStyle, style?.textStyle)
            assertEquals(expectedStyle?.overflow, style?.overflow)
            assertEquals(expectedStyle?.softWrap, style?.softWrap)
            assertEquals(expectedStyle?.maxLines, style?.maxLines)
            assertEquals(expectedStyle?.minLines, style?.minLines)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        fun validateRowStyle(expectedStyle: AepRowStyle?, style: AepRowStyle?) {
            assertEquals(expectedStyle?.verticalAlignment, style?.verticalAlignment)
            assertEquals(expectedStyle?.horizontalArrangement, style?.horizontalArrangement)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        fun validateButtonStyle(expectedStyle: AepButtonStyle?, style: AepButtonStyle?) {
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.enabled, style?.enabled)
            assertEquals(expectedStyle?.elevation, style?.elevation)
            assertEquals(expectedStyle?.border, style?.border)
            assertEquals(expectedStyle?.colors, style?.colors)
            assertEquals(expectedStyle?.contentPadding, style?.contentPadding)
            validateTextStyle(expectedStyle?.buttonTextStyle, style?.buttonTextStyle)
        }

        fun validateIconStyle(expectedStyle: AepIconStyle?, style: AepIconStyle?) {
            assertEquals(expectedStyle?.tint, style?.tint)
            assertEquals(expectedStyle?.contentDescription, style?.contentDescription)
            assertEquals(expectedStyle?.modifier, style?.modifier)
        }

        fun validateCardStyle(expectedStyle: AepCardStyle?, style: AepCardStyle?) {
            assertEquals(expectedStyle?.border, style?.border)
            assertEquals(expectedStyle?.colors, style?.colors)
            assertEquals(expectedStyle?.elevation, style?.elevation)
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.shape, style?.shape)
        }

        fun validateColumnStyle(expectedStyle: AepColumnStyle?, style: AepColumnStyle?) {
            assertEquals(expectedStyle?.modifier, style?.modifier)
            assertEquals(expectedStyle?.horizontalAlignment, style?.horizontalAlignment)
            assertEquals(expectedStyle?.verticalArrangement, style?.verticalArrangement)
        }
    }
}
