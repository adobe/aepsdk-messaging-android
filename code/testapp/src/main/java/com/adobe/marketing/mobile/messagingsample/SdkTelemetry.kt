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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.EdgeCallbackWithError
import com.adobe.marketing.mobile.EdgeEventError
import com.adobe.marketing.mobile.EdgeEventHandle
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.MobileCore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Proof of concept sender for SDK adoption (`sdk.adoption`) and usage (`sdk.usage`) telemetry.
 *
 * The payload follows the "Device Info Schema" (SDK field group). The Edge extension recognizes
 * these two XDM event types and sends them with an empty `identityMap` and without stored state.
 *
 * Uses only Android APIs and public Core and Edge APIs, so it can later move into Core unchanged.
 * Adoption carries the install details: `metadata` (UUID, platform, OS, app, device, permissions),
 * `sdkVersions[]` and the organization id. It is sent once per combination of app version and SDK
 * versions, and is marked as sent only when Edge reports no errors for it. Usage carries only the
 * install UUID, the organization id and `telemetry.events[]`, and is sent as one request per call,
 * without batching. The install details of a usage event come from the adoption event with the same UUID.
 */
object SdkTelemetry {

    enum class ApiEventType { API_CALL, TIMELINE, API_FAIL }

    /** Extension ids, as listed in the Event Hub shared state. */
    object Extensions {
        const val CORE = SdkVersionsExtension.CORE_ID
        const val EDGE = "com.adobe.edge"
        const val MESSAGING = "com.adobe.messaging"
    }

    private const val TAG = "SdkTelemetry"

    /** Tenant namespace of the schema in the Adobe owned sandbox. */
    private const val TENANT = "_cjmstage"

    private const val MAX_TRACKED_REQUESTS = 500
    private const val ADOPTION_EVENT_TYPE = "sdk.adoption"
    private const val USAGE_EVENT_TYPE = "sdk.usage"
    private const val PLATFORM = "android"

    private const val PREFERENCES_NAME = "com.adobe.marketing.mobile.messagingsample.sdktelemetry"
    private const val KEY_INSTALL_ID = "installId"
    private const val KEY_ADOPTION_SENT_FOR = "adoptionSentFor"
    private const val KEY_SIMULATED_APP_VERSION = "simulatedAppVersion"
    private const val KEY_SIMULATED_CORE_VERSION = "simulatedCoreVersion"
    private const val KEY_SIMULATED_EDGE_VERSION = "simulatedEdgeVersion"
    private const val KEY_SIMULATED_MESSAGING_VERSION = "simulatedMessagingVersion"
    private const val KEY_SIMULATED_ORG_ID = "simulatedOrgId"

    /** Extensions that belong to the test app itself and are not part of the SDK. */
    private const val TEST_APP_EXTENSION_PREFIX = "com.adobe.marketing.mobile.messagingsample"

    // Configuration is part of Core and reports the Core version, so it is not listed separately.
    private const val CONFIGURATION_EXTENSION_ID = "com.adobe.module.configuration"
    private const val UNKNOWN_API = "sendEvent"

    /** A well formed datastream id that does not exist, used by [sendEdgeFailureTestEvent]. */
    private const val INVALID_DATASTREAM_ID = "00000000-0000-0000-0000-000000000000"

    private val started = AtomicBoolean(false)

    // Usage events recorded before the final configuration is applied; sent once it is.
    private val pendingUsage = mutableListOf<Map<String, Any>>()

    // Organization id from the final configuration (`experienceCloud.org`).
    @Volatile
    private var orgId: String? = null

    // XDM event type of recent Edge requests, to name the API of a failed request.
    private val requestApiNames = ConcurrentHashMap<String, String>()

    // Unique ids of the Edge request events that carry SDK telemetry.
    private val telemetryRequestEventIds = ConcurrentHashMap.newKeySet<String>()

    // Unique ids of the failed Edge request events already reported as API_FAIL.
    private val reportedFailedRequestEventIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Waits for a Configuration that satisfies [isConfigurationReady], then sends the adoption event
     * (if needed) and a TIMELINE usage event. Sending earlier on a fresh install can race with the
     * Configuration extension, which leaves Edge with an incomplete configuration for the event.
     * Also reports every failed Edge request as an API_FAIL usage event.
     * Call before configuring the SDK so the first Configuration response is not missed.
     */
    @JvmStatic
    fun startWhenConfigured(context: Context, isConfigurationReady: (Map<String, Any?>) -> Boolean) {
        val appContext = context.applicationContext
        trackEdgeFailures(appContext)
        MobileCore.registerEventListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT) { event ->
            val configuration = event.eventData ?: return@registerEventListener
            if (configuration["edge.configId"] == null || !isConfigurationReady(configuration)) {
                return@registerEventListener
            }
            if (started.compareAndSet(false, true)) {
                orgId = configuration["experienceCloud.org"] as? String
                sendAdoptionIfNeeded(appContext)
                trackUsage(appContext, ApiEventType.TIMELINE, Extensions.CORE, "appStart")
                flushPendingUsage(appContext)
            }
        }
    }

    /**
     * Sends an Experience Event to a datastream id that does not exist, so the Edge Network rejects
     * it with an error. Used only to test the API_FAIL path of [trackEdgeFailures].
     */
    @JvmStatic
    fun sendEdgeFailureTestEvent(xdmEventType: String = "sdkTelemetryTest.edgeFailure") {
        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("eventType" to xdmEventType))
            .setDatastreamIdOverride(INVALID_DATASTREAM_ID)
            .build()
        Log.d(TAG, "Sending Edge failure test event to datastream $INVALID_DATASTREAM_ID")
        Edge.sendEvent(experienceEvent, null)
    }

    /**
     * Reports one API_FAIL usage event for each Edge request that returns an error. The API name is
     * the XDM event type of the failed request, the error code is the Edge error type and the error
     * name is the Edge error title. Errors for the telemetry requests themselves are ignored so a
     * failing telemetry request cannot report itself.
     */
    private fun trackEdgeFailures(context: Context) {
        MobileCore.registerEventListener(EventType.EDGE, EventSource.REQUEST_CONTENT) { event ->
            val xdm = event.eventData?.get("xdm") as? Map<*, *>
            val xdmEventType = xdm?.get("eventType")
            if (xdmEventType == ADOPTION_EVENT_TYPE || xdmEventType == USAGE_EVENT_TYPE) {
                telemetryRequestEventIds.add(event.uniqueIdentifier)
            } else {
                if (requestApiNames.size >= MAX_TRACKED_REQUESTS) requestApiNames.clear()
                (xdmEventType as? String)?.let { requestApiNames[event.uniqueIdentifier] = it }
            }
        }
        MobileCore.registerEventListener(EventType.EDGE, EventSource.ERROR_RESPONSE_CONTENT) { event ->
            val requestEventId = event.eventData?.get("requestEventId") as? String
                ?: return@registerEventListener
            if (telemetryRequestEventIds.contains(requestEventId)) {
                return@registerEventListener
            }
            // Edge can dispatch more than one error event for the same request.
            if (reportedFailedRequestEventIds.add(requestEventId)) {
                Log.d(TAG, "Edge request $requestEventId failed, reporting API_FAIL.")
                val errorCode = (event.eventData?.get("type") as? String)?.substringAfterLast('/')
                val errorName = event.eventData?.get("title") as? String
                trackUsage(
                    context,
                    ApiEventType.API_FAIL,
                    Extensions.EDGE,
                    requestApiNames.remove(requestEventId) ?: UNKNOWN_API,
                    errorCode,
                    errorName
                )
            }
        }
    }

    /**
     * Sends the adoption event if it has not been sent yet for the current app version and SDK versions.
     */
    @JvmStatic
    fun sendAdoptionIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val packageInfo = getPackageInfo(appContext)
        val sdkVersions = sdkVersions(appContext)
        val versionKey = (
            listOf(appVersion(appContext, packageInfo).orEmpty()) +
                sdkVersions.map { "${it["name"]}:${it["version"]}" }
            ).joinToString("|")

        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.getString(KEY_ADOPTION_SENT_FOR, null) == versionKey) {
            Log.d(TAG, "Adoption already sent for versions $versionKey, skipping.")
            return
        }

        val metadata = metadata(appContext, packageInfo).apply {
            putIfNotEmpty("permissions", permissions(packageInfo))
        }
        val tenantData = baseTenantData(appContext, metadata, sdkVersions)

        send(ADOPTION_EVENT_TYPE, tenantData, object : EdgeCallbackWithError {
            // onComplete always fires first; onError follows synchronously only when Edge returned errors.
            override fun onComplete(handles: List<EdgeEventHandle>) {
                preferences.edit().putString(KEY_ADOPTION_SENT_FOR, versionKey).apply()
            }

            override fun onError(errors: List<EdgeEventError>) {
                preferences.edit().remove(KEY_ADOPTION_SENT_FOR).apply()
            }
        })
    }

    /**
     * Sends one usage event for a single API call, failure or milestone.
     *
     * @param extension id of the extension that owns the API, for example [Extensions.MESSAGING]
     * @param apiName the API name, or the milestone name for a TIMELINE event
     * @param errorCode the error code, for an API_FAIL event
     * @param errorName the error name, for an API_FAIL event
     */
    @JvmStatic
    @JvmOverloads
    fun trackUsage(
        context: Context,
        apiEventType: ApiEventType,
        extension: String,
        apiName: String,
        errorCode: String? = null,
        errorName: String? = null
    ) {
        val appContext = context.applicationContext
        val telemetryEvent = mutableMapOf<String, Any>(
            "apiEventType" to apiEventType.name,
            "timestamp" to System.currentTimeMillis()
        )
        telemetryEvent.putIfNotEmpty("extension", extension)
        telemetryEvent.putIfNotEmpty("apiName", apiName)
        telemetryEvent.putIfNotEmpty("errorCode", errorCode)
        telemetryEvent.putIfNotEmpty("errorName", errorName)
        val tenantData = baseTenantData(appContext, mutableMapOf<String, Any>("uuid" to installId(appContext)))
        tenantData["telemetry"] = mapOf("events" to listOf(telemetryEvent))

        synchronized(pendingUsage) {
            // Until the final configuration is applied, Edge may send to the wrong datastream or domain.
            if (!started.get()) {
                pendingUsage.add(tenantData)
                return
            }
        }
        send(USAGE_EVENT_TYPE, tenantData)
    }

    private fun baseTenantData(
        context: Context,
        metadata: Map<String, Any>,
        sdkVersions: List<Map<String, String>> = emptyList()
    ): MutableMap<String, Any> {
        val tenantData = mutableMapOf<String, Any>("metadata" to metadata)
        tenantData.putIfNotEmpty("sdkVersions", sdkVersions)
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        tenantData.putIfNotEmpty("orgId", preferences.getString(KEY_SIMULATED_ORG_ID, null) ?: orgId)
        return tenantData
    }

    // Events queued before the configuration arrived have no organization id yet, so it is added here.
    private fun flushPendingUsage(context: Context) {
        val pending = synchronized(pendingUsage) {
            pendingUsage.toList().also { pendingUsage.clear() }
        }
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val currentOrgId = preferences.getString(KEY_SIMULATED_ORG_ID, null) ?: orgId
        pending.forEach {
            val tenantData = it.toMutableMap()
            if (!tenantData.containsKey("orgId")) tenantData.putIfNotEmpty("orgId", currentOrgId)
            send(USAGE_EVENT_TYPE, tenantData)
        }
    }

    private fun send(
        xdmEventType: String,
        tenantData: Map<String, Any>,
        resultCallback: EdgeCallbackWithError? = null
    ) {
        val xdm = mapOf(
            "eventType" to xdmEventType,
            TENANT to tenantData
        )
        val experienceEvent = ExperienceEvent.Builder().setXdmSchema(xdm).build()
        Log.d(TAG, "Sending $xdmEventType: $xdm")
        Edge.sendEvent(experienceEvent, object : EdgeCallbackWithError {
            override fun onComplete(handles: List<EdgeEventHandle>) {
                Log.d(TAG, "$xdmEventType completed with ${handles.size} handle(s).")
                resultCallback?.onComplete(handles)
            }

            override fun onError(errors: List<EdgeEventError>) {
                Log.w(TAG, "$xdmEventType failed: ${errors.joinToString { "${it.status} ${it.title}" }}")
                resultCallback?.onError(errors)
            }
        })
    }

    private fun metadata(context: Context, packageInfo: PackageInfo?): MutableMap<String, Any> {
        val metadata = mutableMapOf<String, Any>(
            "uuid" to installId(context),
            "platform" to PLATFORM
        )
        metadata.putIfNotEmpty("osVersion", Build.VERSION.RELEASE)

        val application = mutableMapOf<String, Any>()
        application.putIfNotEmpty("package", context.packageName)
        application.putIfNotEmpty("version", appVersion(context, packageInfo))
        application.putIfNotEmpty("name", context.applicationInfo.loadLabel(context.packageManager).toString())
        metadata.putIfNotEmpty("application", application)

        // Android has no public API for the marketing model name, so the device code name is used.
        val device = mutableMapOf<String, Any>()
        device.putIfNotEmpty("manufacturer", Build.MANUFACTURER)
        device.putIfNotEmpty("model", Build.MODEL)
        device.putIfNotEmpty("modelName", Build.DEVICE)
        metadata.putIfNotEmpty("device", device)

        return metadata
    }

    /**
     * Core and every registered extension, from the Event Hub shared state. The test app's own
     * extensions are left out. A simulated version replaces the reported one (test only).
     */
    private fun sdkVersions(context: Context): List<Map<String, String>> {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val versions = SdkVersionsExtension.latestVersions.toMutableMap()
        if (versions.isEmpty()) {
            versions[Extensions.CORE] = MobileCore.extensionVersion()
            versions[Extensions.EDGE] = Edge.extensionVersion()
        }
        mapOf(
            Extensions.CORE to KEY_SIMULATED_CORE_VERSION,
            Extensions.EDGE to KEY_SIMULATED_EDGE_VERSION,
            Extensions.MESSAGING to KEY_SIMULATED_MESSAGING_VERSION
        ).forEach { (id, key) ->
            val simulated = preferences.getString(key, null)
            if (simulated != null && versions.containsKey(id)) {
                versions[id] = simulated
            }
        }
        return versions
            .filterKeys { !it.startsWith(TEST_APP_EXTENSION_PREFIX) && it != CONFIGURATION_EXTENSION_ID }
            .filterValues { it.isNotEmpty() }
            .toSortedMap()
            .map { (id, version) -> mapOf("name" to id, "version" to version) }
    }

    private fun appVersion(context: Context, packageInfo: PackageInfo?): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_SIMULATED_APP_VERSION, null) ?: packageInfo?.versionName
    }

    /**
     * Test only: replaces the organization id and the app, Core, Edge and Messaging versions reported in telemetry, to simulate app and
     * SDK upgrades for the POC dataset. Only the reported values change; the SDK itself is not
     * changed. A null value clears that override. The values are kept across app restarts.
     */
    @JvmStatic
    fun setSimulatedVersions(
        context: Context,
        appVersion: String?,
        coreVersion: String?,
        edgeVersion: String?,
        messagingVersion: String?,
        orgId: String? = null
    ) {
        val editor = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
        mapOf(
            KEY_SIMULATED_APP_VERSION to appVersion,
            KEY_SIMULATED_CORE_VERSION to coreVersion,
            KEY_SIMULATED_EDGE_VERSION to edgeVersion,
            KEY_SIMULATED_MESSAGING_VERSION to messagingVersion,
            KEY_SIMULATED_ORG_ID to orgId
        ).forEach { (key, value) ->
            if (value.isNullOrEmpty()) editor.remove(key) else editor.putString(key, value)
        }
        editor.commit()
        Log.d(
            TAG,
            "Simulated versions: app=$appVersion core=$coreVersion edge=$edgeVersion messaging=$messagingVersion org=$orgId"
        )
    }

    private fun permissions(packageInfo: PackageInfo?): List<Map<String, String>> {
        val names = packageInfo?.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags
        return names.mapIndexedNotNull { index, name ->
            if (name.isNullOrEmpty()) {
                return@mapIndexedNotNull null
            }
            val flag = flags?.getOrNull(index) ?: 0
            val granted = flag and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            mapOf("name" to name, "granted" to granted.toString())
        }
    }

    private fun installId(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.getString(KEY_INSTALL_ID, null)?.let { return it }
        val installId = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_INSTALL_ID, installId).apply()
        return installId
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            val packageManager = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Unable to read package info: ${e.message}")
            null
        }
    }

    // The schema requires every string to be non-empty, so empty values are left out.
    private fun <V> MutableMap<String, V>.putIfNotEmpty(key: String, value: Any?) {
        val isEmpty = when (value) {
            null -> true
            is String -> value.isEmpty()
            is Map<*, *> -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            else -> false
        }
        if (!isEmpty) {
            @Suppress("UNCHECKED_CAST")
            put(key, value as V)
        }
    }
}
