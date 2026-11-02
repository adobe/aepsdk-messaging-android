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

import android.util.Log
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

/**
 * Test app extension that records every Event Hub event and every shared state change as complete
 * JSON, one record per line, in `files/aep_capture/records.jsonl` of the app's private storage.
 * Read it with `adb exec-out run-as <package> cat files/aep_capture/records.jsonl`.
 *
 * Core logs only the owner of a changed shared state, not its content. Logcat truncates each entry
 * at about 4 KB and drops writes during large bursts, so complete records go to the file and only a
 * one-line summary goes to logcat (tag [TAG]).
 */
class DebugCaptureExtension(extensionApi: ExtensionApi) : Extension(extensionApi) {

    private val outputFile: File? =
        ServiceProvider.getInstance().appContextService.applicationContext?.let { context ->
            File(context.filesDir, OUTPUT_PATH).apply { parentFile?.mkdirs() }
        }

    override fun getName(): String = "com.adobe.marketing.mobile.messagingsample.debugcapture"

    override fun getFriendlyName(): String = "DebugCapture"

    override fun getVersion(): String = "1.0.0"

    override fun onRegistered() {
        api.registerEventListener(EventType.WILDCARD, EventSource.WILDCARD, ::handleEvent)
    }

    private fun handleEvent(event: Event) {
        val eventRecord = JSONObject()
            .put("name", event.name)
            .put("type", event.type)
            .put("source", event.source)
            .put("id", event.uniqueIdentifier)
            .put("parentId", event.parentID)
            .put("responseId", event.responseID)
            .put("timestamp", event.timestamp)
            .put("data", event.eventData?.let { JSONObject(it) })
        writeRecord("event", eventRecord, "${event.name} (${event.type} / ${event.source})")

        if (event.type == EventType.HUB && event.source == EventSource.SHARED_STATE) {
            val owner = event.eventData?.get(STATE_OWNER) as? String ?: return
            val sharedStateRecord = JSONObject()
                .put("owner", owner)
                .put("changeEventName", event.name)
                .put("timestamp", event.timestamp)
                .put("standard", toJson(api.getSharedState(owner, event, false, SharedStateResolution.ANY)))
                .put("xdm", toJson(api.getXDMSharedState(owner, event, false, SharedStateResolution.ANY)))
            writeRecord("sharedState", sharedStateRecord, "$owner (${event.name})")
        }
    }

    private fun toJson(result: SharedStateResult?): JSONObject? {
        result ?: return null
        return JSONObject()
            .put("status", result.status.name)
            .put("value", result.value?.let { JSONObject(it) })
    }

    @Synchronized
    private fun writeRecord(kind: String, payload: JSONObject, summary: String) {
        val record = JSONObject()
            .put("kind", kind)
            .put("loggedAt", System.currentTimeMillis())
            .put("payload", payload)
            .toString()
        try {
            outputFile?.let { file -> FileWriter(file, true).use { it.write(record + "\n") } }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to write capture record: ${e.message}")
        }
        Log.i(TAG, "$kind: $summary (${record.length} chars)")
    }

    companion object {
        const val TAG = "AEPCapture"
        const val OUTPUT_PATH = "aep_capture/records.jsonl"
        private const val STATE_OWNER = "stateowner"
    }
}
