# Adobe Experience Platform - Messaging extension for Android

[![CircleCI](https://img.shields.io/circleci/project/github/adobe/aepsdk-messaging-android/main.svg?logo=circleci)](https://circleci.com/gh/adobe/workflows/aepsdk-messaging-android)
[![Code Coverage](https://codecov.io/gh/adobe/aepsdk-messaging-android/branch/staging-v2.0.0/graph/badge.svg?token=3RLMTJQ1TM)](https://codecov.io/gh/adobe/aepsdk-messaging-android)

## About this project
The AEPMessaging extension enables sending and tracking push notifications in the Adobe Experience Platform

## Installation

The Messaging SDK is available from the Sonatype snapshot repository while it is in beta. In your app's top level Gradle file, add a reference to the repository:

```groovy
allprojects {
  repositories {
    // other needed repositories...
    // add the sonatype snapshot repository
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
  }
} 
```

Integrate the AEPMessaging extension into your app by including the following in your app level gradle file's `dependencies`:

```groovy
implementation 'com.adobe.marketing.mobile:messaging:2.0.0-SNAPSHOT'
implementation 'com.adobe.marketing.mobile:edge:2.0.0-SNAPSHOT'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.0.0-SNAPSHOT'
implementation 'com.adobe.marketing.core:2.0.0-SNAPSHOT'
```

If you use the Messaging extension (In-App beta) alongside the Campaign Standard extension, Campaign Standard extension version 2.0.0 or newer must be used to resolve a compatibility issue:

```groovy
implementation 'com.adobe.marketing.mobile:campaign:2.0.0-SNAPSHOT'
```

Adding Firebase messaging sdk as it is required for using [FCM](https://firebase.google.com/docs/cloud-messaging/android/client#add_firebase_sdks_to_your_app)
```
implementation 'com.google.firebase:firebase-messaging:<latest-version>'
```

### Development

**Open the project**

To open and run the project, open the `code/build.gradle` file in Android Studio

**Run demo application**
- Follow this [Firebase documentation](https://firebase.google.com/docs/cloud-messaging/android/client#add_a_firebase_configuration_file) to add the configuration file for your firebase project. 
- Once you opened the project in Android Studio (see above), select the `app` runnable and your favorite emulator and run the program.

## Documentation
Additional documentation for configuration and sdk usage can be found under the [Documentation](Documentation/README.md) directory.

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [AEPEdgeIdentity Extension](https://github.com/adobe/aepsdk-edgeidentity-android) | The AEPEdgeIdentity enables handling of user identity data from a mobile app when using the AEPEdge extension. |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK.                 |
| [AEP SDK Sample App for iOS](https://github.com/adobe/aepsdk-sample-app-ios) | Contains iOS sample apps for the AEP SDK. Apps are provided for both Objective-C and Swift implementations. |

## Contributing
Contributions are welcomed! Read the [CONTRIBUTING](.github/CONTRIBUTING.md) for more information.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
