# Message (Interface)

The `Message` interface contains the definition of an in-app message and provides a framework to track message interactions via Experience Edge events.

`InternalMessage` objects implementing this interface are created by the AEPMessaging extension, and passed as the `message` parameter in `MessagingDelegate` protocol methods.

## Public functions

### show

Signals to the UIService that the message should be shown.

If `autoTrack` is true, calling this method will result in an "decisioning.propositionDisplay" Edge Event being dispatched.

```java
void show()
```

### dismiss

Signals to the UIService that the message should be removed from the UI.

If `autoTrack` is true, calling this method will result in an "decisioning.propositionDismiss" Edge Event being dispatched.

```java
void dismiss(final boolean suppressAutoTrack)
```

###### Parameters

* *suppressAutoTrack* - if set to `true`, the "decisioning.propositionDismiss" Edge Event will not be sent regardless of the `autoTrack` setting.

### track

Generates and dispatches an Edge Event for the provided `interaction` and `eventType`.

```java
void track(final String interaction, final MessagingEdgeEventType eventType)
```

###### Parameters

* *interaction* - a custom `String` value to be recorded in the interaction
* _eventType_ - the [`MessagingEdgeEventType`](./../enum-public-classes/enum-messaging-edge-event-type.md) to be used for the ensuing Edge Event

### getAutoTrack

Retrieves the `Message's` auto tracking preference.

```java
default boolean getAutoTrack()
```

### setAutoTrack

Sets the `Message's` auto tracking preference.

```java
void setAutoTrack(boolean enabled)
```

###### Parameters

* *enabled* - if true, Experience Edge events will automatically be generated when this `Message` is triggered, displayed, or dismissed.

### evaluateJavascript

Evaluates the passed in `String` content containing javascript code using the `Message's ` webview. `handleJavascriptMessage` must be called with a valid callback before calling `evaluateJavascript` as the body of the message passed from the javascript code execution will be returned in the `AdobeCallback` .

```java
void evaluateJavascript(final String content)
```

###### Parameters

* *content* - a string containing the javascript code to be executed

### handleJavascriptMessage

Adds a handler for named JavaScript messages sent from the message's `WebView`.

The  `AdobeCallback` will contain the body of the message passed from the `WebView`'s JavaScript.

For a full guide on how to use `handleJavascriptMessage`, read [Call native code from the Javascript of an in-app message](./how-to-call-native-from-javascript.md).

```java
void handleJavascriptMessage(final String name, final AdobeCallback<String> callback)
```

###### Parameters

* *name* - the name of the message that should be handled by the `callback`
* *callback* - a callback which will be called with the body of the message created in the Message's JavaScript

### getId

Returns the message's id.

```java
String getId()
```

### getParent

Returns the `Object` which created this `Message`.

```java
Object getParent()
```

### getWebView

Returns a reference to the message's  `WebView`  instance, if it exists.

```java
WebView getWebView()
```

### String values

Below is the table of values returned by calling the `toString` method for each case, which are used as the XDM `eventType` in outgoing experience events:

| Case                      | String value                      |
| ------------------------- | --------------------------------- |
| `IN_APP_DISMISS`          | `decisioning.propositionDismiss`  |
| `IN_APP_INTERACT`         | `decisioning.propositionInteract` |
| `IN_APP_TRIGGER`          | `decisioning.propositionTrigger`  |
| `IN_APP_DISPLAY`          | `decisioning.propositionDisplay`  |
| `PUSH_APPLICATION_OPENED` | `pushTracking.applicationOpened`  |
| `PUSH_CUSTOM_ACTION`      | `pushTracking.customAction`       |
