#  Getting started with Messaging SDK

## Integrate Messaging extension in the mobile app

### [Gradle](https://gradle.org/)

Add the dependencies to `build.gradle` for Messaging extension.

```java
implementation 'com.adobe.marketing.mobile:assurance:2.+'
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
implementation "com.adobe.marketing.mobile:messaging:2.3.0-cbe-beta-SNAPSHOT'
```

Add maven snapshots repository URL to `repositories` in the buildscript.
```java
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url "https://plugins.gradle.org/m2/" }
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
    }
}
```

## Import and register the Messaging extension

Import the Messaging extension and its dependencies, then register the Messaging extension and dependencies in the `onCreate` method in the `Application` class:

```java
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
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
        MobileCore.setLogLevel(LoggingMode.DEBUG);

        MobileCore.registerExtensions(Arrays.asList(Assurance.EXTENSION, Edge.EXTENSION, Identity.EXTENSION, Messaging.EXTENSION), 
            new AdobeCallback () {
                @Override
                public void call(Object o) {
                    // use the Environment file ID assigned for this application from Adobe Data Collection (formerly Adobe Launch)
                    MobileCore.configureWithAppID("<YOUR_ENVIRONMENT_FILE_ID>");
                }
        });
    }
    ...
}
```