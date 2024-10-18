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

package com.adobe.marketing.mobile.aepcomposeui.aepui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.aepui.utils.UIUtils.getColor
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText

/**
 * A composable function that displays a text element with customizable properties.
 *
 * @param defaultStyle The default style to be applied to the text element.
 * @param overriddenStyle The style provided by app that overrides the default style.
 */
@Composable
internal fun AepText.Composable(
    defaultStyle: AepTextStyle? = null,
    overriddenStyle: AepTextStyle? = null
) {
    // Convert server color from string to Color object
    val textColor = color?.getColor()

    // Convert server text alignment from string to TextAlign object
    val textAlign = when (align?.lowercase()) {
        "left" -> TextAlign.Left
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        else -> null
    }

    // Convert server font properties from string to respective font objects
    // map from `font.name` if needed
    val fontFamily = null
    val fontSize = (font?.size)?.sp
    val fontWeight = when (font?.weight?.lowercase()) {
        "bold" -> FontWeight.Bold
        else -> null
    }
    val fontStyle = if (font?.style?.contains("italic") == true) {
        FontStyle.Italic
    } else {
        null
    }

    // Merge all text styles together
    val defaultTextStyle = defaultStyle?.textStyle ?: TextStyle()
    val mergedStyle =
        defaultTextStyle
            .merge(textColor?.let { TextStyle(color = it) })
            .merge(textAlign?.let { TextStyle(textAlign = it) })
            .merge(fontFamily?.let { TextStyle(fontFamily = it) })
            .merge(fontWeight?.let { TextStyle(fontWeight = it) })
            .merge(fontSize?.let { TextStyle(fontSize = it) })
            .merge(fontStyle?.let { TextStyle(fontStyle = it) })
            .merge(overriddenStyle?.textStyle)

    // Text Composable
    Text(
        text = content,
        style = mergedStyle,
        modifier = overriddenStyle?.modifier ?: defaultStyle?.modifier ?: Modifier,
        overflow = overriddenStyle?.overflow ?: defaultStyle?.overflow ?: TextOverflow.Clip,
        softWrap = overriddenStyle?.softWrap ?: defaultStyle?.softWrap ?: true,
        maxLines = overriddenStyle?.maxLines ?: defaultStyle?.maxLines ?: Int.MAX_VALUE,
        minLines = overriddenStyle?.minLines ?: defaultStyle?.minLines ?: 1
    )
}
