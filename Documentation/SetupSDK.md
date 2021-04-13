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
