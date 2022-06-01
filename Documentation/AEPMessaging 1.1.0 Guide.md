# AEPMessaging 1.1.0 Push Notification Improvements

AEPMessaging 1.1.0 introduces AEPMessaging handled push notification creation and tracking. The new functionality provides a convenient way to display and track notification interactions with an AJO created push notification. The steps below will serve as a guide to enabling the new functionaity provided in AEPMessaging 1.1.0.

{% hint style="info" %}

The usage of these features are optional and no code changes are required if not using them. 

{% hint style="info" %}

Push notification creation must be handled by the AEPMessaging extension if the AEPMessaging will be handling push notification interaction tracking. To provide some additional flexibility in this scenario, two public interfaces are provided which allow custom push notification factory (`IMessagingPushNotificationFactory`) and notification image downloading (`IMessagingImageDownloader`) instances. After these interfaces are implemented on the app side, they can be set via the new `setPushNotificationFactory` and `setPushImageDownloader` API.

{% hint style="warning" %}

Ensure that your custom push notification factory correctly sets a `PendingIntent` object for each content intent, delete intent, or button when creating a push notification if the AEPMessaging extension will be used to handle push notification interactions.

#### Step 1: Adding the AEPMessaging receivers in the App manifest

Within your App's `AndroidManifest.xml` file, add a [manifest-declared receiver](https://developer.android.com/guide/components/broadcasts#manifest-declared-receivers) inside the application tag:

```groovy
 <!--messaging extension handled push notification broadcast receiver-->
   <receiver
			android:name=".NotificationBroadcastReceiver"
			android:exported="false">
      <intent-filter>
    		<action android:name="${applicationId}_adb_action_notification_clicked" />
        <action android:name="${applicationId}_adb_action_button_clicked" />
        <action android:name="${applicationId}_adb_action_notification_deleted" />
        <action android:name="${applicationId}_adb_action_notification_created" />
        <action android:name="${applicationId}_adb_action_silent_notification_created" />
      </intent-filter>
	</receiver>

<!--messaging extension handled push notification interactions broadcast receiver-->
  <receiver
			android:name="com.adobe.marketing.mobile.MessagingPushInteractionHandler"
      android:exported="false" >
  </receiver>
```

The intent filters within the first receiver above can be added to an existing mainfest-declared `BroadcastReceiver` to listen for any notification events sent by the  AEPMessaging extension when it is handling push notification creation. The `MessagingPushInteractionHandler` receiver must be added if the AEPMessaging extension is handling push interaction tracking.

#### Step 2: Call the new AEPMessaging push notification creation API

In your App's class which extends `FirebaseMessagingService`, pass the data payload and a boolean (signaling if tracking should be handled) to the new `handlePushNotificationWithRemoteMessage` API:

```java
public void onMessageReceived(RemoteMessage message) {
  super.onMessageReceived(message);
  // AEPMessaging handling of the push payload. If shouldHandleTracking is true then the AEPMessaging extension 	will handle push notification interaction tracking automatically.
  Messaging.handlePushNotificationWithRemoteMessage(message, true);
}
```

#### Step 3: Add/Update a Broadcast Receiver object to listen for AEPMessaging created push notification broadcasts

The AEPMessaging extension will broadcast events on normal notification creation, silent notification creation, notification deletion, notification click, or notification button presses. A `BroadcastReceiver` [must be declared in the AndroidManifest.xml](#Step 1: Adding the AEPMessaging receivers in the App manifest) and a class subclassing `BroadcastReceiver` must be added to handle the broadcasted events:

```java
public void onReceive(Context context, Intent intent) {
  String action = intent != null ? intent.getAction() : null;
  String packageName = context != null ? context.getPackageName() : null;
  // these values are broadcast when a silent push notification is handled by the Messaging extension
  MessagingPushPayload pushPayload = null;
  String messageId = null;
  
  if (action != null) {
    if (packageName != null) {
      if (action.equals(packageName + "_adb_action_notification_clicked")) {
        Log.d("NotificationBroadcast", action);
      } else if (action.equals(packageName + "_adb_action_notification_deleted")) {
        Log.d("NotificationBroadcast", packageName + "_adb_action_notification_deleted");
      } else if (action.equals(packageName + "_adb_action_button_clicked")) {
        Log.d("NotificationBroadcast", packageName + "_adb_action_button_clicked");
      } else if (action.equals(packageName + "_adb_action_notification_created")) {
        Log.d("NotificationBroadcast", packageName + "_adb_action_notification_created");
      } else if (action.equals(packageName + "_adb_action_silent_notification_created")) {
        Log.d("NotificationBroadcast", packageName + "_adb_action_silent_notification_created");
        Bundle extras = intent.getExtras();
        if (extras != null) {
          pushPayload = (MessagingPushPayload) extras.getParcelable("pushPayload");
          messageId = extras.getString("messageId");
         }
      }
    }
  }
}
```



