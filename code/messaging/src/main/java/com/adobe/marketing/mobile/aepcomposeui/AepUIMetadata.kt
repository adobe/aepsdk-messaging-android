/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui

/**
 * Delivery metadata of the proposition an [AepUI] was built from.
 *
 * Kept separate from the UI template so that presentation data and ordering data can evolve independently.
 *
 * @property priority The priority configured in the AJO UI, from 0 (lowest) to 100 (highest). Defaults to 0.
 * @property rank The ordinal rank assigned by the server for the surface; a lower rank is delivered first.
 * Defaults to -1, which means the rank was not present in the response.
 */
data class AepUIMetadata(
    val priority: Int = DEFAULT_PRIORITY,
    val rank: Int = DEFAULT_RANK
) {
    companion object {
        const val DEFAULT_PRIORITY = 0
        const val DEFAULT_RANK = -1
    }
}
