# Message

The `Message` class contains the definition of an in-app message and controls its tracking via Experience Edge events.

`Message` objects are only created by the AEPMessaging extension, and passed as the `message` parameter in `MessagingDelegate` protocol methods.

## Public variables

### id

Identifier of the `Message`.

```java
public String id;
```

### autoTrack

If set to `true` (default), Experience Edge events will automatically be generated when this `Message` is triggered, displayed, and dismissed.

```java
public boolean autoTrack = true;
```

## Public functions

### show

Signals to the UIService that the message should be shown.

If `autoTrack` is true, calling this method will result in an "decisioning.propositionDisplay" Edge Event being dispatched.

```java
public void show()
```

### dismiss

Signals to the UIService that the message should be removed from the UI.

If `autoTrack` is true, calling this method will result in an "decisioning.propositionDismiss" Edge Event being dispatched.

```java
public void dismiss(final boolean suppressAutoTrack)
```

###### Parameters

* *suppressAutoTrack* - if set to `true`, the "decisioning.propositionDismiss" Edge Event will not be sent regardless of the `autoTrack` setting.

### track

Generates and dispatches an Edge Event for the provided `interaction` and `eventType`.

```java
public void track(final String interaction, final MessagingEdgeEventType eventType)
```

###### Parameters

* *interaction* - a custom `String` value to be recorded in the interaction
* *eventType* - the [`MessagingEdgeEventType`](#enum-messagingedgeeventtype) to be used for the ensuing Edge Event

### handleJavascriptMessage

Adds a handler for named JavaScript messages sent from the message's `WebView`.

The  `AdobeCallback` will contain the body of the message passed from the `WebView`'s JavaScript.

For a full guide on how to use `handleJavascriptMessage`, read [Call native code from the Javascript of an in-app message](./how-to-call-native-from-javascript.md).

```java
public void handleJavascriptMessage(final String name, final AdobeCallback<String> callback)
```

###### Parameters

* *name* - the name of the message that should be handled by the `callback`
* *callback* - a callback which will be called with the body of the message created in the Message's JavaScript

### getWebView

Returns a reference to the message's  `WebView`  instance, if it exists.

```java
public WebView getWebView()
```

### String values

Below is the table of values returned by calling the `toString` method for each case, which are used as the XDM `eventType` in outgoing experience events:

| Case                    | String value                      |
| ----------------------- | --------------------------------- |
| IN_APP_DISMISS          | `decisioning.propositionDismiss`  |
| IN_APP_INTERACT         | `decisioning.propositionInteract` |
| IN_APP_TRIGGER          | `decisioning.propositionTrigger`  |
| IN_APP_DISPLAY          | `decisioning.propositionDisplay`  |
| PUSH_APPLICATION_OPENED | `pushTracking.applicationOpened`  |
| PUSH_CUSTOM_ACTION      | `pushTracking.customAction`       |
