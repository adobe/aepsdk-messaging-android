# Enum - MessagingEdgeEventType

Provides mapping to XDM EventType strings needed for Experience Event requests.

This enum is used in conjunction with the [`track`](./class-message.md#track) method of a `Message` object.

### Definition

```java
public enum MessagingEdgeEventType {
    PUSH_APPLICATION_OPENED(4),
    PUSH_CUSTOM_ACTION(5),
    DISMISS(6),
    INTERACT(7),
    TRIGGER(8),
    DISPLAY(9);

    static final String PROPOSITION_EVENT_TYPE_DISMISS = "dismiss";
    static final String PROPOSITION_EVENT_TYPE_INTERACT = "interact";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER = "trigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY = "display";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED =
            "pushTracking.applicationOpened";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION =
            "pushTracking.customAction";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER_STRING = "decisioning.propositionTrigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY_STRING = "decisioning.propositionDisplay";
    static final String PROPOSITION_EVENT_TYPE_INTERACT_STRING = "decisioning.propositionInteract";
    static final String PROPOSITION_EVENT_TYPE_DISMISS_STRING = "decisioning.propositionDismiss";

    final int value;

    MessagingEdgeEventType(final int value) {
        this.value = value;
    }

    /**
     * @deprecated This method will be removed in future versions.
     */
    @Deprecated
    public int getValue() {
        return value;
    }

    public String getPropositionEventType() {
        switch (this) {
            case DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS;
            case INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT;
            case TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER;
            case DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY;
            default:
                return "";
        }
    }

    @NonNull @Override
    public String toString() {
        switch (this) {
            case DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS_STRING;
            case INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT_STRING;
            case TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER_STRING;
            case DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY_STRING;
            case PUSH_APPLICATION_OPENED:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED;
            case PUSH_CUSTOM_ACTION:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION;
            default:
                return super.toString();
        }
    }
}
```
