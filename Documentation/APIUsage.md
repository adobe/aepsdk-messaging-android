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

## Sending feedback about push notification interactions. 

### Fields for push notification tracking.
| Key               | dataType   | Description                                                                                                                    |
|-------------------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| eventType         | String     | Type of event when push notification  interaction happens Values: - pushTracking.applicationOpened - pushTracking.customAction |
| id                | String     | MessageId for the push notification                                                                                            |
| applicationOpened | boolean    | Whether application was opened or not                                                                                          |
| actionId          | String     | actionId of the element which performed  the custom action.                                                                    |
| adobe             | Dictionary | Adobe related information.                                                                                                     |

##### Sending feedback when application is opened without any custom action. To do this, add the following code where you have access to `intent` after the user interact with the push notification:
```kotlin
intent?.extras?.let {
    val messageId = it.getString("messageId", "")
    val adobeData = it.getString("_xdm", "")
    val map = mutableMapOf<String, Any>()
    map["eventType"] = "pushTracking.applicationOpened"
    map["id"] = messageId
    map["applicationOpened"] = true
    map["adobe"] = adobeData
    MobileCore.collectMessageInfo(map)
}
```

```kotlin
intent?.extras?.let {
    val messageId = it.getString("messageId", "")
    val adobeData = it.getString("_xdm", "")
    val map = mutableMapOf<String, Any>()
    map["eventType"] = "pushTracking.applicationOpened"
    map["id"] = messageId
    map["applicationOpened"] = true
    map["adobe"] = adobeData
    MobileCore.collectMessageInfo(map)
}
```

```java
Intent intent = getIntent();
Bundle bundle = intent.getExtras();
if (bundle != null) {
    String messageId = bundle.getString("messageId", "");
    String adobeData = bundle.getString("_xdm", "");
    HashMap<String, Object> info = new HashMap<>();
    info.put("eventType", "pushTracking.applicationOpened");
    info.put("id", messageId);
    info.put("applicationOpened", true);
    info.put("adobe", adobeData);
    MobileCore.collectMessageInfo(info);
 }
```

##### Sending feedback when application is opened with custom action. To do this, add the following code where you have access to `intent` after the user interact with the push notification:
```kotlin
intent?.extras?.let {
    val messageId = it.getString("messageId", "")
    val adobeData = it.getString("_xdm", "")
    val map = mutableMapOf<String, Any>()
    map["eventType"] = "pushTracking.customAction"
    map["id"] = messageId
    map["actionId"] = "<actionId>"
    map["applicationOpened"] = true
    map["adobe"] = adobeData
    MobileCore.collectMessageInfo(map)
}
```

```java
Intent intent = getIntent();
Bundle bundle = intent.getExtras();
if (bundle != null) {
    String messageId = bundle.getString("messageId", "");
    String adobeData = bundle.getString("_xdm", "");
    HashMap<String, Object> info = new HashMap<>();
    info.put("eventType", "pushTracking.customAction");
    info.put("id", messageId);
    info.put("actionId", "<actionId>");
    info.put("applicationOpened", true);
    info.put("adobe", adobeData);
    MobileCore.collectMessageInfo(info);
 }
```

##### Sending feedback when application is not opened but a custom action is performed by the user. To do this, add the following code where you have access to `intent` after the user interact with the push notification:
```kotlin
intent?.extras?.let {
    val messageId = it.getString("messageId", "")
    val adobeData = it.getString("_xdm", "")
    val map = mutableMapOf<String, Any>()
    map["eventType"] = "pushTracking.customAction"
    map["id"] = messageId
    map["actionId"] = "<actionId>"
    map["adobe"] = adobeData
    MobileCore.collectMessageInfo(map)
}
```

```java
Intent intent = getIntent();
Bundle bundle = intent.getExtras();
if (bundle != null) {
    String messageId = bundle.getString("messageId", "");
    String adobeData = bundle.getString("_xdm", "");
    HashMap<String, Object> info = new HashMap<>();
    info.put("eventType", "pushTracking.customAction");
    info.put("id", messageId);
    info.put("actionId", "<actionId>");
    info.put("adobe", adobeData);
    MobileCore.collectMessageInfo(info);
 }
```
