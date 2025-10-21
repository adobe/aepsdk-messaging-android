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

package com.adobe.marketing.mobile.aepcomposeui.uimodels

import androidx.compose.ui.Alignment

data class InboxContainerUITemplate(
    override val heading: AepText,
    override val capacity: Int,
    override val emptyMessage: AepText? = null,
    override val emptyImage: AepImage? = null,
    override val unreadBgColor: AepColor? = null,
    override val unreadIcon: AepImage? = null,
    override val unreadIconAlignment: Alignment? = null
) : AepContainerUITemplate(
    heading = heading,
    layout = "vertical",
    capacity = capacity,
    emptyMessage = emptyMessage,
    emptyImage = emptyImage,
    isUnreadEnabled = true,
    unreadBgColor = unreadBgColor,
    unreadIcon = unreadIcon,
    unreadIconAlignment = unreadIconAlignment
) {
    override fun getType() = AepContainerUIType.INBOX
}
