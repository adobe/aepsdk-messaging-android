# Adobe Experience Platform - Messaging extension for Android (Beta feature - Experience Decisioning in Code Based Experiences)

[![CircleCI](https://img.shields.io/circleci/project/github/adobe/aepsdk-messaging-android/main.svg?logo=circleci)](https://circleci.com/gh/adobe/workflows/aepsdk-messaging-android)
[![Code Coverage](https://codecov.io/gh/adobe/aepsdk-messaging-android/branch/main/graph/badge.svg?token=3RLMTJQ1TM)](https://codecov.io/gh/adobe/aepsdk-messaging-android)

## Beta feature acknowledgment

By using the AEPMessaging SDK (“Beta feature”), you hereby acknowledge that the Beta is provided “as is” without warranty of any kind. Adobe shall have no obligation to maintain, correct, update, change, modify or otherwise support the Beta. You are advised to use caution and not to rely in any way on the correct functioning or performance of such Beta and/or accompanying materials.

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

If you use the Messaging extension alongside the Campaign Standard extension, Campaign Standard extension version 2.0.0 or newer must be used to resolve a compatibility issue:

```groovy
implementation 'com.adobe.marketing.mobile:campaign:2.+'
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

## Tutorial

A comprehensive tutorial for getting started with In-app messaging can be found [here](https://opensource.adobe.com/aepsdk-messaging-android/#/tutorials/README).

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core extensions](https://github.com/adobe/aepsdk-core-android) | The Mobile Core represents the foundation of the Adobe Experience Platform Mobile SDK. |
| [Edge Network extension](https://github.com/adobe/aepsdk-edge-android) | The Edge Network extension allows you to send data to the Adobe Experience Platform (AEP) from a mobile application. |
| [Identity for Edge Network extension](https://github.com/adobe/aepsdk-edgeidentity-android) | The Identity for Edge Network extension enables identity management from a mobile app when using the Edge Network extension. |

## Contributing
Contributions are welcomed! Read the [CONTRIBUTING](.github/CONTRIBUTING.md) for more information.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
