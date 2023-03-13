# Adobe Experience Platform - Messaging extension for Android

[![CircleCI](https://img.shields.io/circleci/project/github/adobe/aepsdk-messaging-android/main.svg?logo=circleci)](https://circleci.com/gh/adobe/workflows/aepsdk-messaging-android)
[![Code Coverage](https://codecov.io/gh/adobe/aepsdk-messaging-android/branch/main/graph/badge.svg?token=3RLMTJQ1TM)](https://codecov.io/gh/adobe/aepsdk-messaging-android)

## About this project
The AEPMessaging extension enables sending and tracking push notifications in the Adobe Experience Platform

## Installation

Integrate the AEPMessaging extension into your app by including the following in your app level gradle file's `dependencies`:

```groovy
implementation 'com.adobe.marketing.mobile:messaging:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
implementation 'com.adobe.marketing.core:2.+'
```

### Development

**Open the project**

To open and run the project, open the `code/build.gradle` file in Android Studio

**Set your Data Collection Environment ID and your Assurance session link**

In Android Studio open the `MessagingApplication` class within the `messagingsample` project. There are two predefined variables at the beginning of the class:
```kotlin
private val ENVIRONMENT_FILE_ID = "3149c49c3910/4f6b2fbf2986/launch-7d78a5fd1de3-development"
private val ASSURANCE_SESSION_LINK = "YOUR-SESSION-LINK"
```

You can modify these two variables with your own environment file id and/or assurance session link if needed.

**Run demo application**

- Once you have opened the project in Android Studio (see above) and setup the configuration variables, select the `app` runnable and your favorite emulator and run the program.

## Documentation
Additional documentation for configuration and sdk usage can be found under the [Documentation](Documentation/README.md) directory.

## Tutorial

A comprehensive tutorial for getting started with In-app messaging can be found [here](https://opensource.adobe.com/aepsdk-messaging-android/#/tutorials/README).

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
