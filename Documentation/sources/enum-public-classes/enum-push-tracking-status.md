# PushTrackingStatus

Enum representing the status of push tracking.

### Definition

```java
public enum PushTrackingStatus {
    TRACKING_INITIATED,
    NO_DATASET_CONFIGURED,
    NO_TRACKING_DATA,
    INVALID_INTENT,
    INVALID_MESSAGE_ID,
    UNKNOWN_ERROR;
}
```


| Enum                    | Description                       |
| ----------------------- | --------------------------------- |
| `TRACKING_INITIATED`        | This status is returned when all the required data for tracking is available and tracking is initiated.  |
| `NO_DATASET_CONFIGURED`       | This status is returned when tracking is not initiated because no tracking dataset is configured. |
| `NO_TRACKING_DATA`        | This status is returned when tracking is not initiated because the intent does not contain tracking data.|
| `INVALID_INTENT`        | This status is returned when tracking is not initiated because the intent is invalid.  |
| `INVALID_MESSAGE_ID` | This status is returned when tracking is not initiated because the message id is invalid.  |
| `UNKNOWN_ERROR`    | This status is returned when tracking is not initiated because of an unknown error.      |
