# Manual display and tracking of push notification

## Overview
This document describes how to manually display and track push notifications using the AEPMessaging extension.

## Sync the push token

To register the push token with Adobe Experience Platform, add the following code to the Application class's `onCreate()` method:

```java
public class YourApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {

            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    MobileCore.setPushIdentifier(token);
                }       
            }
        });
    }
}
```

## Buidling and Displaying notification

1. In `onMessageReceived` method of `YourAppFirebaseMessagingService` class, create a `MessagingPushPayload` object from the remoteMessage. `MessagingPushPayload` will unpack the remoteMessage and provide APIs for getting attributes used for creating the push notification.

    ```java
    MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);
    ```

2. Use the [Public API's](../enum-public-classes/messaging-push-payload.md) of MessagingPushPayload to get the attributes required for creating the push notification.

    ```java
    // Following are a few examples of using the public APIs of MessagingPushPayload
    String title = payload.getTitle();
    String body = payload.getBody();
    int badgeCount = payload.getBadgeCount();
    int notificationPriority = payload.getNotificationPriority();
    String channelId = payload.getChannelId();
    String icon = payload.getIcon();
    String imageUrl = payload.getImageUrl();
    ```

3. The intent which is created while building the notification needs to be updated with necessary Adobe information to track push notification interactions.

    ```java
    Messaging.addPushTrackingDetails(
        final Intent intent, // intent which will be used when user interacts with the notification.
        final String messageId, // message.id which represents the id of the push notification
        final Map<String, String> data) // message.data which represents the data part of the remoteMessage.
    ```

4. Use NotificationManager to create and display the built notification.

Here is a sample code for creating a notification channel and building the notification.
```java
public class YourAppFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Create a MessagingPushPayload object from the remoteMessage
        MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);

        // Write your code for creating or using an existing notification channel.

        // Creating an intent and attaching tracking details to it.
        Intent intent = new Intent(this, MainActivity.class);
        Messaging.addPushTrackingDetails(intent, remoteMessage.getMessageId(), remoteMessage.getData());
        
        // Write your code to create a pending intent
        
        // Create a notification builder 
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(payload.getTitle())
                        .setContentText(payload.getBody())
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        // Write your code to add custom buttons to notification, if neccessary.

        // Write your code to download and attach media to notification, if neccessary.

        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
```

## Tracking push notification interactions

After the application is opened by the user by clicking on the push notification, use the `handleNotificationResponse` API to send the push notification interactions feedback to platform.

```java
    Messaging.handleNotificationResponse(
        final Intent intent, // Intent which contains information related to messageId and data
        final boolean applicationOpened, //  whether application was opened or not
        final String actionId) // actionId of the element which performed the custom action.
```

##### Sending push notification interaction feedback when application is opened without any custom action. 
Add the following code where you have access to `intent` after the user has interacted with the push notification:

```java
Messaging.handleNotificationResponse(intent, true, null);
```

##### Sending feedback when application is opened with a custom action. 
Similar to the example above, call the `handleNotificationResponse` API but this time with a custom action:

```java
Messaging.handleNotificationResponse(intent, true, <actionId>);
```

##### Sending feedback when application is not opened but a custom action is performed by the user. 
Add the following code where you have access to `intent` after the user has interacted with the push notification:

```java
Messaging.handleNotificationResponse(intent, false, <actionId>);
```

Here is a sample code for sending push notification interactions feedback to platform.

```java

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent intent = getIntent();

        // use your unique way to determine isAppOpenFromPushInteraction
        if (isAppOpenFromPushInteraction) {
            // tracking application opened with no custom action
            Messaging.handleNotificationResponse(intent, true, null);
        }
    }
}

```
