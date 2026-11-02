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

package com.adobe.marketing.mobile.messagingsample

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution

/**
 * Test app extension that keeps the latest Event Hub shared state, which lists Core and every
 * registered extension with its version. [SdkTelemetry] reads it to build `sdkVersions[]`.
 * Core would do the same in production, because the Event Hub state is the one source of truth
 * for what is registered.
 */
class SdkVersionsExtension(extensionApi: ExtensionApi) : Extension(extensionApi) {

    override fun getName(): String = "com.adobe.marketing.mobile.messagingsample.sdkversions"

    override fun getFriendlyName(): String = "SdkVersions"

    override fun getVersion(): String = "1.0.0"

    override fun onRegistered() {
        api.registerEventListener(EventType.HUB, EventSource.SHARED_STATE) { event ->
            if (event.eventData?.get(STATE_OWNER) == EVENT_HUB) {
                refresh(event)
            }
        }
        refresh(null)
    }

    private fun refresh(event: Event?) {
        val state = api.getSharedState(EVENT_HUB, event, false, SharedStateResolution.ANY)?.value
            ?: return
        val extensions = state["extensions"] as? Map<*, *> ?: return
        val versions = linkedMapOf<String, String>()
        (state["version"] as? String)?.let { versions[CORE_ID] = it }
        extensions.forEach { (id, details) ->
            val version = (details as? Map<*, *>)?.get("version") as? String
            if (id is String && version != null) {
                versions[id] = version
            }
        }
        latestVersions = versions
    }

    companion object {
        const val CORE_ID = "com.adobe.module.core"
        private const val EVENT_HUB = "com.adobe.module.eventhub"
        private const val STATE_OWNER = "stateowner"

        /** Extension id to version, from the latest Event Hub shared state. */
        @Volatile
        var latestVersions: Map<String, String> = emptyMap()
            private set
    }
}
