#  API Usage

## General APIs

## Getting the extension version.

Call the `extensionVersion` API to retrieve a `String` containing the Messaging extension's version.

```java
Messaging.extensionVersion()
```

## Push Messaging APIs

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

## In-App Messaging APIs

#### Programmatically refresh in-app message definitions from the remote

By default, the SDK will automatically fetch in-app message definitions from the remote at the time the Messaging extension is registered. This generally happens once per app lifecycle.

Some use cases may require the client to request an update from the remote more frequently. Calling the following API will force the Messaging extension to get an updated definition of messages from the remote:

```java
Messaging.refreshInAppMessages();
```

## Code-based experiences and content cards APIs

### updatePropositionsForSurfaces

Dispatches an event for the Edge network extension to fetch personalization decisions from the AJO campaigns for the provided `Surface`s array. The returned decision `Proposition`s are cached in-memory by the Messaging extension.

To retrieve previously cached decision `Proposition`s, use `getPropositionsForSurfaces` API.

#### Java

##### Syntax

```java
public static void updatePropositionsForSurfaces(@NonNull final List<Surface> surfaces)
```

##### Example

```java
final Surface surface1 = new Surface("myActivity#button");
final Surface surface2 = new Surface("myActivityAttributes");

final List<Surface> surfaces = new ArrayList<>();
surfaces.add(surface1);
surfaces.add(surface2);

Messaging.updatePropositionsForSurfaces(surfaces)
```

### getPropositionsForSurfaces

Retrieves the previously fetched propositions from the SDK's in-memory propositions cache for the provided surfaces. The callback is invoked with the decision propositions corresponding to the given surfaces or `AdobeError`, if it occurs. 

If a requested surface was not previously cached prior to calling `getPropositionsForSurfaces` (using the `updatePropositionsForSurfaces` API), no propositions will be returned for that surface.

#### Java

##### Syntax

```java
public static void getPropositionsForSurfaces(@NonNull final List<Surface> surfaces, @NonNull final AdobeCallback<Map<Surface, List<MessagingProposition>>> callback)
```

##### Example

```java
final Surface surface1 = new Surface("myActivity#button");
final Surface surface2 = new Surface("myActivityAttributes");

final List<Surface> surfaces = new ArrayList<>();
surfaces.add(surface1);
surfaces.add(surface2);

Messaging.getPropositionsForSurfaces(surfaces, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
    @Override
    public void fail(final AdobeError adobeError) {
        // handle error
    }

    @Override
    public void call(Map<Surface, List<MessagingProposition>> propositionsMap) {
        if (propositionsMap != null && !propositionsMap.isEmpty()) {
            // get the propositions for the given surfaces
            if (propositionsMap.contains(surface1)) {
                final List<MessagingProposition> propositions1 = propositionsMap.get(surface1)
                // read surface1 propositions
            }
            if (propositionsMap.contains(surface2)) {
                final List<MessagingProposition> proposition2 = propositionsMap.get(surface2)
                // read surface2 propositions
            }
        }
    }
});
```
