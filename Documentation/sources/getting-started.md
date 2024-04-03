#  Getting started with Messaging SDK

## Integrate Messaging extension in the mobile app

### [Gradle](https://gradle.org/)

Installation via [Maven](https://maven.apache.org/) & [Gradle](https://gradle.org/) is the easiest and recommended way to get the Mobile SDK. Add the dependencies to `build.gradle.kts` for Messaging extension.

#### Kotlin

```kotlin
    implementation(platform("com.adobe.marketing.mobile:sdk-bom:3.+"))
    implementation("com.adobe.marketing.mobile:core")
    implementation("com.adobe.marketing.mobile:assurance")
    implementation("com.adobe.marketing.mobile:edge")
    implementation("com.adobe.marketing.mobile:edgeidentity")
    implementation("com.adobe.marketing.mobile:messaging")
```

#### Groovy

```groovy
    implementation platform('com.adobe.marketing.mobile:sdk-bom:3.+')
    implementation 'com.adobe.marketing.mobile:core'
    implementation 'com.adobe.marketing.mobile:assurance'
    implementation 'com.adobe.marketing.mobile:edge'
    implementation 'com.adobe.marketing.mobile:edgeidentity'
    implementation 'com.adobe.marketing.mobile:messaging'
```

## Import and register the Messaging extension

Import the Messaging extension and its dependencies, then register the Messaging extension and dependencies in the `onCreate` method in the `Application` class:

```java
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
...
import java.util.Arrays;
...
import android.app.Application;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCore.setApplication(this);
        MobileCore.setLogLevel(LoggingMode.VERBOSE);

        MobileCore.registerExtensions(Arrays.asList(Assurance.EXTENSION, Edge.EXTENSION, Identity.EXTENSION, Messaging.EXTENSION, Lifecycle.EXTENSION), 
            new AdobeCallback () {
                @Override
                public void call(Object o) {
                    // use the Environment file ID assigned for this application from Adobe Data Collection (formerly Adobe Launch)
                    MobileCore.configureWithAppID("<YOUR_ENVIRONMENT_FILE_ID>");
                    MobileCore.lifecycleStart(null);
                }
        });
    }
    ...
}
```

```kotlin
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MobileCore
import android.app.Application

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        MobileCore.registerExtensions(listOf(Assurance.EXTENSION, Edge.EXTENSION, Identity.EXTENSION, Messaging.EXTENSION, Lifecycle.EXTENSION)) {
            // use the Environment file ID assigned for this application from Adobe Data Collection (formerly Adobe Launch)
            MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
            MobileCore.lifecycleStart(null)
        }
    }
}
```

#### Next Step
Checkout the API usage [here](./api-usage.md)