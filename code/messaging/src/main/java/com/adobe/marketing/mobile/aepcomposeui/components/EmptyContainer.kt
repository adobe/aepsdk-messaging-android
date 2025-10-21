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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * Composable that renders an empty container if the empty message or image is provided.
 */
@Composable
fun EmptyInboxContainer(
    emptyMessage: AepText? = null,
    emptyMessageStyle: AepTextStyle = AepTextStyle(),
    emptyImage: AepImage? = null,
    emptyImageStyle: AepImageStyle = AepImageStyle()
) {
    if (emptyMessage != null || emptyImage != null) {
        // Wrap AepText in an invisible Surface to provide Material Theme context
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                emptyMessage?.let {
                    AepText(
                        model = it,
                        textStyle = emptyMessageStyle
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                emptyImage?.let {
                    AepAsyncImage(
                        image = it,
                        imageStyle = emptyImageStyle
                    )
                }
            }
        }
    }
}
