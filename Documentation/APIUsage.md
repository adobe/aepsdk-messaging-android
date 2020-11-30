#  API Usage

## Syncing the push token to profile in platform. 

To do this, add the following code to Application classes's `onCreate()` method:
```kotlin
    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
        if(task.isSuccessful) {
            val token = task.result?.token ?: ""
            MobileCore.setPushIdentifier(token)
        }
    })
```

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

## Sending push notification interactions feedback. 

### Updating the intent with necessary Adobe informations.
The intent which is passed while building the notification needs to be updated with necessary adobe informations this is needed so that the information can be passed back while tracking the push notification interactions. 
To do this add the following code to `FirebaseMessagingService#onMessageReceived(message: RemoteMessage?)` method.

``` kotlin
/**
 * intent which will be received by the app when user interacts with the notification.
 * message.messageId is the id of the notification in the remoteMessage
 * message.data which represents the data part of the remoteMessage. 
 */
Messaging.addPushTrackingDetails(intent, message.messageId, message.data)
```

```java
/**
 * intent which will be received by the app when user interacts with the notification.
 * message.messageId is the id of the notification in the remoteMessage
 * message.data which represents the data part of the remoteMessage. 
 */
Messaging.addPushTrackingDetails(intent, message.getMessageId(), message.getData())
```

### Sending push notification interactions details 
| Key               | dataType   | Description                                                                                                                    |
|-------------------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| intent            | Intent     | Intent which contains information related to messageId and data.                                                                                      |
| applicationOpened | boolean    | Whether application was opened or not                                                                                          |
| actionId          | String     | actionId of the element which performed  the custom action.                                                                    |

##### Sending feedback when application is opened without any custom action. To do this, add the following code where you have access to `intent` after the user has interacted with the push notification:
```kotlin
Messaging.handleNotificationResponse(intent, true, null)
```

```java
Messaging.handleNotificationResponse(intent, true, null);
```

##### Sending feedback when application is opened with custom action. To do this, add the following code where you have access to `intent` after the user interact with the push notification:
```kotlin
Messaging.handleNotificationResponse(intent, true, <actionId>)
```

```java
Messaging.handleNotificationResponse(intent, true, <actionId>);
```

##### Sending feedback when application is not opened but a custom action is performed by the user. To do this, add the following code where you have access to `intent` after the user interact with the push notification:
```kotlin
Messaging.handleNotificationResponse(intent, false, <actionId>)
```

```java
Messaging.handleNotificationResponse(intent, false, <actionId>);
```
