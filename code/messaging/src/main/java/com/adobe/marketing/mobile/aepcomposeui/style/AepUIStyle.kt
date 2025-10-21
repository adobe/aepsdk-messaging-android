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

import com.adobe.marketing.mobile.aepcomposeui.components.ImageOnlyCard
import com.adobe.marketing.mobile.aepcomposeui.components.LargeImageCard
import com.adobe.marketing.mobile.aepcomposeui.components.SmallImageCard

/**
 * Enumerates the style configuration for all supported types of AEP UI components.
 *
 * @param smallImageUIStyle The [SmallImageUIStyle] with configuration for [SmallImageCard].
 * @param smallImageUIStyle The [LargeImageUIStyle] with configuration for [LargeImageCard].
 * @param imageOnlyUIStyle Thr [ImageOnlyUIStyle] with configuration for [ImageOnlyCard].
 */
class AepUIStyle(
    val smallImageUIStyle: SmallImageUIStyle = SmallImageUIStyle.Builder().build(),
    val largeImageUIStyle: LargeImageUIStyle = LargeImageUIStyle.Builder().build(),
    val imageOnlyUIStyle: ImageOnlyUIStyle = ImageOnlyUIStyle.Builder().build()
)
