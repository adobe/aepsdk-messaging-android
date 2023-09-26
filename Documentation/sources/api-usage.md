# Push Notification APIs

## Syncing the push token to profile in platform. 

Add the following code to the Application class's `onCreate()` method:

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
