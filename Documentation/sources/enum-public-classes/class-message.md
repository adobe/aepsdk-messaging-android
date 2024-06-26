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

```java
void dismiss()
```

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

### getId

Returns the message's id.

```java
String getId()
```

Below is the table of values returned by calling the `toString` method for each case, which are used as the XDM `eventType` in outgoing experience events:

| Case                      | String value                      |
| ------------------------- | --------------------------------- |
| `DISMISS`                 | `decisioning.propositionDismiss`  |
| `INTERACT`                | `decisioning.propositionInteract` |
| `TRIGGER`                 | `decisioning.propositionTrigger`  |
| `DISPLAY`                 | `decisioning.propositionDisplay`  |
| `PUSH_APPLICATION_OPENED` | `pushTracking.applicationOpened`  |
| `PUSH_CUSTOM_ACTION`      | `pushTracking.customAction`       |
