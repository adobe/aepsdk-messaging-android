#  Setting up AEPMessaging SDK

#### Import messaging extension in the Application file:
```java
import com.adobe.marketing.mobile.*;
```

```kotlin
import com.adobe.marketing.mobile.*
```

#### Registering the extension
Register the messaging extensions and configure the SDK with the assigned application identifier. To do this, add the following code to the Application class's `onCreate()` method:

```kotlin
override fun onCreate() {
    MobileCore.setApplication(this)
    MobileCore.setLogLevel(LoggingMode.DEBUG)
    
    Messaging.registerExtension()
    Identity.registerExtension()
    UserProfile.registerExtension()
    Lifecycle.registerExtension()
    Signal.registerExtension()
    Edge.registerExtension()

    MobileCore.start {
        MobileCore.configureWithAppID("<appId>")
    }
}
```

```java
@Override
public void onCreate() {
    MobileCore.setApplication(application);
    MobileCore.setLogLevel(LoggingMode.VERBOSE);
    
    Messaging.registerExtension();
    Identity.registerExtension();
    UserProfile.registerExtension();
    Lifecycle.registerExtension();
    Signal.registerExtension();
    Edge.registerExtension();
    
    MobileCore.start(new AdobeCallback() {
        @Override
        public void call(Object value) {
           MobileCore.configureWithAppID("<appId>");
        }
    });
}
```

#### Updating the configuration 
To update the configuration with the required DCCS url, add the following code to the Application class's `onCreate()` method:

```kotlin
override fun onCreate() {
    MobileCore.start {
        MobileCore.configureWithAppID("<appId>")
        MobileCore.updateConfiguration(mutableMapOf("messaging.dccs" to "<DCCS_URL>") as Map<String, Any>?)
    }
}
```

```java
@Override
public void onCreate() {
    MobileCore.start(new AdobeCallback() {
        @Override
        public void call(Object value) {
           MobileCore.configureWithAppID("<appId>");
           HashMap<String, Object> configuration = new HashMap<>();
           configuration.put("messaging.dccs", "<DCCS_URL>");
           MobileCore.updateConfiguration(configuration);
        }
    });
}
```
