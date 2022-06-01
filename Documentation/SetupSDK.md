#  Setting up the AEPMessaging SDK

#### Import messaging extension in the Application class:

```java
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
```

#### Registering the extension
Register the messaging extensions and configure the SDK with the launch application identifier. To do this, add the following code to the Application class's `onCreate()` method:

```java
@Override
public void onCreate() {
    MobileCore.setApplication(application);
    
    Messaging.registerExtension();
    Edge.registerExtension();
    Identity.registerExtension();
    
    
    MobileCore.start(new AdobeCallback() {
        @Override
        public void call(Object value) {
           MobileCore.configureWithAppID("<appId>");
        }
    });
}
```

#### Next Step
Checkout the API usage [here](APIUsage.md)
