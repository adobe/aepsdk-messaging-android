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

package com.adobe.marketing.mobile.messaging;

/**
 * Describes where a content card surface's currently-loaded rules originated.
 *
 * <p>This is a provenance signal — it answers "were these cards read from the persisted disk
 * cache?" — and intentionally not a connectivity/freshness signal. It is tracked in memory only
 * (see {@code EdgePersonalizationResponseHandler.contentCardOriginByProposition}) and is never
 * persisted, so it cannot leak into or corrupt on-disk data.
 */
enum CardOrigin {
    /** Rules were loaded from a successful live personalization (network) response. */
    NETWORK,

    /** Rules were hydrated from the persisted on-disk content card cache. */
    DISK
}
