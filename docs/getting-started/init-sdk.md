# Initialize the Adobe Experience Platform Mobile SDKs

Initialize the Experience Platform Mobile SDKs by adding the below code in your `Application` class.

> [!TIP]
> You can find your Environment File ID and SDK initialization code in your _Tag_ property in the _Experience Platform Data Collection_ UI. <br /><br />Navigate to **Environments** > select your environment (**Production**, **Staging**, or **Development**) > click **INSTALL**.

**Import messaging extension in the Application class**

```kotlin
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.LoggingMode
```

**Registering the extension**

```kotlin
override fun onCreate() {
  super.onCreate()
  MobileCore.setApplication(this)
  MobileCore.setLogLevel(LoggingMode.VERBOSE)
  MobileCore.registerExtensions(listOf(Messaging.EXTENSION, Identity.EXTENSION, Edge.EXTENSION, Assurance.EXTENSION)) {
    MobileCore.configureWithAppID("MY_APP_ID")
    MobileCore.lifecycleStart(null)
  }
}
```
