# Adobe Experience Platform - Messaging extension for Android

## About this project
The AEPMessaging extension, along with Adobe Customer Journey Optimizer, enables sending and tracking push notifications in the Adobe Experience Platform

## Installation
Integrate the AEPMessaging extension into your app by including the following in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:messaging:1.3.0'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:1.+'
implementation 'com.adobe.marketing.mobile:sdk-core:1.+'
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
