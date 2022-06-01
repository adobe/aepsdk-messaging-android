#  API Usage

## Syncing the push token to profile in platform. 

To do this, add the following code to Application class's `onCreate()` method:

```java
    FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    MobileCore.setPushIdentifier(token);
                }
            }
        });
```

## Sending push notification interactions feedback to platform. 

### Updating the intent with necessary Adobe information.
The intent which is passed while building the notification needs to be updated with necessary Adobe information to track push notification interactions. 
To do this add the following code to `FirebaseMessagingService#onMessageReceived(message: RemoteMessage?)` method.


```java
/**
 * intent which will be used when user interacts with the notification.
 * message.messageId is the id of the push notification
 * message.data which represents the data part of the remoteMessage. 
 * returns boolean value indicating whether the intent was update with push tracking details (messageId and xdm data).
 */
boolean update = addPushTrackingDetails(final Intent intent, final String messageId, final Map<String, String> data)
```

### Sending push notification interactions details 
| Key               | dataType   | Description                                                                                                                    |
|-------------------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| intent            | Intent     | Intent which contains information related to messageId and data.                                                                                      |
| applicationOpened | boolean    | Whether application was opened or not                                                                                          |
| actionId          | String     | actionId of the element which performed  the custom action.                                                                    |

##### Sending push notification interaction feedback when application is opened without any custom action. 
To do this, add the following code where you have access to `intent` after the user has interacted with the push notification:

```java
Messaging.handleNotificationResponse(intent, true, null);
```

##### Sending feedback when application is opened with custom action. 
To do this, add the following code where you have access to `intent` after the user interact with the push notification:

```java
Messaging.handleNotificationResponse(intent, true, <actionId>);
```

##### Sending feedback when application is not opened but a custom action is performed by the user. 
To do this, add the following code where you have access to `intent` after the user interact with the push notification:

```java
Messaging.handleNotificationResponse(intent, false, <actionId>);
```
## AEPMessaging 1.1.0 Push Notification Improvements

### Messaging extension push notification creation

In the `FirebaseMessagingService#onMessageReceived` function of your app, invoke `handlePushNotificationWithRemoteMessage` with the remote message received from firebase cloud messaging (FCM). Additionally, a boolean which signals if tracking should be handled by the Messaging extension is required when invoking the API.

| Key                  | dataType      | Description                                                  |
| -------------------- | ------------- | ------------------------------------------------------------ |
| remoteMessage        | RemoteMessage | Remote message received from FCM containing an AJO push data notification |
| shouldHandleTracking | boolean       | Signals if the Messaging extension should handle push notification tracking |

```java
public void onMessageReceived(RemoteMessage message) {
  super.onMessageReceived(message);
  Messaging.handlePushNotificationWithRemoteMessage(message, true);
}
```

### Push notification creation customization

The Messaging extension defines two interfaces which can be implemented to customize the Messaging extension's creation of push notifications as well as customize the downloading of push notification image assets.

##### Setting a custom push notification factory

```java
// customFactory is an instance of a class which implements IMessagingPushNotificationFactory
Messaging.setPushNotificationFactory(customFactory);
```

##### Setting a custom push image downloader

```java
// customImageDownloader is an instance of a class which implements IMessagingImageDownloader
Messaging.setPushImageDownloader(customImageDownloader);
```

