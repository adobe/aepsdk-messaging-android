# AJO Push Notification Guide - Android
## End-to-End Setup & Triggering Push Notifications

**Author:** Ritu Singh  
**Last Updated:** May 2026  
**Environment:** Adobe Staging (`experience-stage.adobe.com`)

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Firebase Setup](#2-firebase-setup)
3. [Android App Configuration](#3-android-app-configuration)
4. [AEP Mobile SDK Integration](#4-aep-mobile-sdk-integration)
5. [AJO Channel Configuration](#5-ajo-channel-configuration)
6. [Create API-Triggered Campaign in AJO](#6-create-api-triggered-campaign-in-ajo)
7. [Verify Profile & Push Token in AEP](#7-verify-profile--push-token-in-aep)
8. [Trigger Push Notification via cURL](#8-trigger-push-notification-via-curl)
9. [Trigger Push Notification via Postbuster](#9-trigger-push-notification-via-postbuster)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Prerequisites

- Android Studio with an emulator running a **Google APIs** system image (not "Google Play" — FCM needs Google APIs)
- Firebase project with Cloud Messaging enabled
- Access to Adobe Experience Platform (AEP) staging environment
- Access to Adobe Journey Optimizer (AJO)
- AEP Mobile SDK dependencies in your Android project

### Key Identifiers (Your Current Setup)

| Item | Value |
|------|-------|
| Firebase Project | `testing-campaign-7f2af` |
| App ID | `com.adobe.marketing.mobile.messagingsample` |
| AEP Org ID | `745F37C35E4B776E0A49421B@AdobeOrg` |
| Edge Config ID | `9485aa93-4312-4eab-8741-dcdac24e356b` |
| Launch Environment ID | `staging/1b50a869c4a2/8d83ca76a48a/launch-98809790b968-development` |
| Edge Environment | `int` |
| Sandbox | `prod` |
| API Key | `dx_cjm_mr_integration_tests` |
| AJO Channel Config | `Ritu-push-config` |
| Campaign ID | `a3f1d776-f761-4630-82fb-97ea378b3f94` |

---

## 2. Firebase Setup

### 2a. Create Firebase Project (one-time)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing: `testing-campaign-7f2af`)
3. Add an Android app with package name: `com.adobe.marketing.mobile.messagingsample`
4. Download `google-services.json` and place it in `testapp/` directory

### 2b. Enable FCM V1 API

1. In Firebase Console, go to **Project Settings > Cloud Messaging**
2. Ensure **Firebase Cloud Messaging API (V1)** is **Enabled**
3. If it shows "disabled", click the link to enable it in Google Cloud Console

### 2c. Generate FCM V1 Service Account Key

1. In Firebase Console, go to **Project Settings > Service Accounts**
2. Click **Generate New Private Key**
3. Save the JSON file — you'll upload this to AJO channel configuration later

> **IMPORTANT:** AJO requires FCM **V1** credentials (service account JSON), NOT the legacy server key. The legacy API is deprecated/disabled.

---

## 3. Android App Configuration

### 3a. Gradle Dependencies

Ensure these are in your `testapp/build.gradle`:

```groovy
// Firebase
implementation platform('com.google.firebase:firebase-bom:<latest>')
implementation 'com.google.firebase:firebase-messaging'

// AEP SDK
implementation 'com.adobe.marketing.mobile:core:<latest>'
implementation 'com.adobe.marketing.mobile:messaging:<latest>'
implementation 'com.adobe.marketing.mobile:edge:<latest>'
implementation 'com.adobe.marketing.mobile:edgeidentity:<latest>'
implementation 'com.adobe.marketing.mobile:lifecycle:<latest>'
implementation 'com.adobe.marketing.mobile:assurance:<latest>'
```

### 3b. AndroidManifest.xml

Ensure you have:
- Internet permission
- Firebase messaging service declaration
- Deep link intent filters (for Assurance, if needed)

### 3c. Firebase Messaging Service (`NotificationService.kt`)

```kotlin
class NotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MobileCore.setPushIdentifier(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // AEP SDK handles the push display
        MessagingService.handleRemoteMessage(this, message)
    }
}
```

---

## 4. AEP Mobile SDK Integration

### 4a. Application Class (`MessagingApplication.kt`)

This is the critical initialization file:

```kotlin
class MessagingApplication : Application() {
    private val ENVIRONMENT_FILE_ID = "staging/1b50a869c4a2/8d83ca76a48a/launch-98809790b968-development"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        val extensions = listOf(
            Messaging.EXTENSION,
            Identity.EXTENSION,
            Lifecycle.EXTENSION,
            Edge.EXTENSION,
            Assurance.EXTENSION
        )

        MobileCore.registerExtensions(extensions) {
            MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
            MobileCore.updateConfiguration(
                hashMapOf("edge.environment" to "int") as Map<String, Any>
            )
            MobileCore.lifecycleStart(null)

            // CRITICAL: Must be false to force push token sync on every app launch
            val configMap = mapOf(
                "messaging.optimizePushSync" to false
            )
            MobileCore.updateConfiguration(configMap)
        }
    }
}
```

> **CRITICAL SETTING:** `messaging.optimizePushSync` must be `false`. When `true`, the SDK skips sending the push token to AEP if it thinks it hasn't changed, which can prevent your profile from having a push token.

### 4b. Setting the Push Identifier

The push token must be sent to AEP. This happens in two ways:

1. **Automatically** via `NotificationService.onNewToken()` calling `MobileCore.setPushIdentifier(token)`
2. **Manually** via a button in the app (useful for testing):

```kotlin
// In MainActivity.kt
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        MobileCore.setPushIdentifier(task.result)
    }
}
```

### 4c. Build & Install

```bash
# Build the app
./gradlew :testapp:assembleDebug

# Install on emulator
adb install -r testapp/build/outputs/apk/debug/testapp-debug.apk
```

### 4d. Verify SDK Events in Logcat

After launching the app, check Logcat for these critical events:

```
# Filter by: "Edge" or "Messaging"

# Look for these success indicators:
- "Edge request sent" — Edge network connection working
- "Push token synced" — FCM token sent to AEP
- "ECID: <your-ecid>" — Identity established
```

---

## 5. AJO Channel Configuration

1. Go to **AJO** > **Administration** > **Channels** > **Channel Configurations**
2. Create or edit a push channel configuration (e.g., `Ritu-push-config`)
3. Set:
   - **Platform:** Android
   - **App ID:** `com.adobe.marketing.mobile.messagingsample`
   - **FCM Credentials:** Upload the **FCM V1 service account JSON** file (from Step 2c)
4. Save the configuration

> **NOTE:** If you previously used Legacy FCM server key, you MUST update to V1 credentials. Legacy API is deprecated.

---

## 6. Create API-Triggered Campaign in AJO

### 6a. Create the Campaign

1. Go to **AJO** > **Campaigns** > **Create Campaign**
2. Select **API-triggered**
3. Give it a name (e.g., "Ritu Push Test Campaign")

### 6b. Configure the Action

1. Add action: **Push Notification**
2. Select your channel configuration: `Ritu-push-config`
3. Design the push notification:
   - **Title:** Your notification title
   - **Body:** Your notification body
4. Set identity namespace to **ECID**

### 6c. Activate the Campaign

1. Review the campaign summary
2. Click **Activate**
3. Note the **Campaign ID** — you'll need this for the API call

> Your current Campaign ID: `a3f1d776-f761-4630-82fb-97ea378b3f94`

---

## 7. Verify Profile & Push Token in AEP

Before triggering the campaign, verify that your device's profile exists in AEP with a push token.

### 7a. Get your ECID

From Logcat, search for ECID:
```
adb logcat | grep -i "ecid"
```

Or in the app, if it displays the ECID in the UI.

### 7b. Check Profile in AEP

1. Go to **AEP** > **Profiles**
2. Search by **Identity namespace: ECID** and paste your ECID
3. Verify the profile exists
4. Check the **Events** tab — you should see Edge events (lifecycle, push token sync)

> If no profile is found, restart the app and tap "Set Push Identifier" to force Edge events to flow.

---

## 8. Trigger Push Notification via cURL

### 8a. Get a Fresh Access Token

In your AJO browser tab (`experience-stage.adobe.com`), open DevTools (F12) > Console and run:

```javascript
JSON.parse(sessionStorage.getItem('adobeid/745F37C35E4B776E0A49421B@AdobeOrg/production/access_token')).value
```

Copy the entire token string. This token expires in ~1 hour.

### 8b. Send the cURL Request

```bash
curl -X POST 'https://platform-stage.adobe.io/ajo/im/executions/unitary' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <YOUR_FRESH_TOKEN>' \
  -H 'x-gw-ims-org-id: 745F37C35E4B776E0A49421B@AdobeOrg' \
  -H 'x-api-key: dx_cjm_mr_integration_tests' \
  -H 'x-sandbox-name: prod' \
  -d '{
    "requestId": "test-push-UNIQUE-ID",
    "campaignId": "a3f1d776-f761-4630-82fb-97ea378b3f94",
    "recipients": [
      {
        "userId": "<YOUR_ECID>",
        "namespace": "ECID",
        "type": "aep"
      }
    ]
  }'
```

> **IMPORTANT:** Change `requestId` to a unique value every time (e.g., `test-push-003`, `test-push-004`, etc.). AJO rejects duplicate request IDs.

### 8c. Expected Response

**Success (200 OK):**
```json
{
  "requestId": "test-push-003",
  "status": "ACCEPTED"
}
```

The notification should appear on your emulator within 5-30 seconds.

---

## 9. Trigger Push Notification via Postbuster

### 9a. Create a New Request

1. Open Postbuster
2. Create a new request
3. Set method: **POST**
4. Set URL: `https://platform-stage.adobe.io/ajo/im/executions/unitary`

### 9b. Auth Tab

1. Select **Bearer Token**
2. Paste your fresh access token (from Step 8a)

> **DO NOT** also add an Authorization header in the Headers tab — the Auth tab handles it automatically. Having it in both places causes a 400 error.

### 9c. Headers Tab

Add **exactly** these 4 headers (no more, no less):

| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |
| `x-gw-ims-org-id` | `745F37C35E4B776E0A49421B@AdobeOrg` |
| `x-api-key` | `dx_cjm_mr_integration_tests` |
| `x-sandbox-name` | `prod` |

> **DO NOT add:** `Authorization` (handled by Auth tab) or `x-sandbox-id` (not needed)

### 9d. Body Tab

Select **raw** > **JSON** and enter:

```json
{
  "requestId": "test-push-UNIQUE-ID",
  "campaignId": "a3f1d776-f761-4630-82fb-97ea378b3f94",
  "recipients": [
    {
      "userId": "<YOUR_ECID>",
      "namespace": "ECID",
      "type": "aep"
    }
  ]
}
```

### 9e. Send

Click **Send**. Expected: **200 OK** with `"status": "ACCEPTED"`.

> Remember to change `requestId` to a new unique value each time!

---

## 10. Troubleshooting

### Common Issues & Fixes

| Problem | Cause | Fix |
|---------|-------|-----|
| **401 Authentication failed** | Token expired or wrong token type | Get a fresh personal token from browser sessionStorage (Step 8a). Tokens expire in ~1 hour. |
| **403 Api Key is invalid** | Wrong API key | Use `dx_cjm_mr_integration_tests` as the `x-api-key` |
| **400 Bad Request (openresty)** | Duplicate Authorization header | Remove `Authorization` from Headers tab; only use Auth tab |
| **400 JSON parsing error** | Wrong request body format | Ensure `recipients` is an **array** with objects containing `userId`, `namespace`, `type` |
| **404 Cannot find the Profile** | ECID has no profile in AEP | Restart app, tap "Set Push Identifier", wait 1-2 min, then retry |
| **Campaign shows "Actions failed: 1"** | FCM delivery failed | Check AJO channel config has FCM **V1** credentials (not Legacy) |
| **No notification on emulator** | Firebase broken on emulator | Clear app data: `adb shell pm clear com.adobe.marketing.mobile.messagingsample`, relaunch app |
| **Push token not syncing** | SDK optimization skipping sync | Ensure `messaging.optimizePushSync` is `false` in MessagingApplication.kt |
| **FIS_AUTH_ERROR in Logcat** | Firebase Installation Service corrupted | Clear app data with `adb shell pm clear ...`, relaunch |
| **ECID changed after clearing data** | Expected behavior | Get the new ECID from Logcat, wait for profile to appear in AEP, update the `userId` in your API request |

### Quick Recovery Checklist

If notifications stop working, go through this checklist:

1. **Is your token fresh?** Get a new one from sessionStorage (expires every ~1 hour)
2. **Is the ECID still the same?** Check Logcat — if the emulator was wiped, ECID changes
3. **Does the profile exist in AEP?** Search by ECID in AEP Profiles
4. **Does the profile have a push token?** Check Events tab in the profile
5. **Is the campaign still active?** Check campaign status in AJO
6. **Is FCM working?** Check Logcat for Firebase errors
7. **Did you change the requestId?** AJO rejects duplicate request IDs

### Useful ADB Commands

```bash
# Check app logs for AEP SDK events
adb logcat | grep -i "edge\|messaging\|ecid\|push\|firebase"

# Clear app data (resets Firebase + ECID — use as last resort)
adb shell pm clear com.adobe.marketing.mobile.messagingsample

# Reinstall the app
adb install -r testapp/build/outputs/apk/debug/testapp-debug.apk
```

### Getting the Access Token (Quick Reference)

Open browser console on `experience-stage.adobe.com` and run:

```javascript
JSON.parse(sessionStorage.getItem('adobeid/745F37C35E4B776E0A49421B@AdobeOrg/production/access_token')).value
```

---

## Quick Reference: Send a Push Notification in 60 Seconds

If everything is already set up and the app is running:

1. Get fresh token from browser console (see above)
2. Run cURL:

```bash
curl -X POST 'https://platform-stage.adobe.io/ajo/im/executions/unitary' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <FRESH_TOKEN>' \
  -H 'x-gw-ims-org-id: 745F37C35E4B776E0A49421B@AdobeOrg' \
  -H 'x-api-key: dx_cjm_mr_integration_tests' \
  -H 'x-sandbox-name: prod' \
  -d '{
    "requestId": "push-'$(date +%s)'",
    "campaignId": "a3f1d776-f761-4630-82fb-97ea378b3f94",
    "recipients": [{"userId":"<YOUR_ECID>","namespace":"ECID","type":"aep"}]
  }'
```

> **Pro tip:** Using `$(date +%s)` as part of the requestId auto-generates a unique ID based on timestamp.

---

*This guide was created based on the complete setup and debugging session for AJO push notifications with the AEP Messaging SDK test app.*
