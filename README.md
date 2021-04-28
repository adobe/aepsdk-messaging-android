# Adobe Experience Platform - Messaging extension for Android

## BETA
Adobe Experience Platform - Messaging is currently in BETA. Use of this code is by invitation only and not otherwise supported by Adobe. Please contact your Adobe Customer Success Manager to learn more.

By using the Beta, you hereby acknowledge that the Beta is provided "as is" without warranty of any kind. Adobe shall have no obligation to maintain, correct, update, change, modify or otherwise support the Beta. You are advised to use caution and not to rely in any way on the correct functioning or performance of such Alpha and/or accompanying materials.

## About this project
The AEPMessaging extension allows you to send push notification tokens and push notification click-through feedback to the Adobe Experience Platform.

## Current version
The Adobe Experience Platform Messaging extension for Android is currently in Beta development.

## Installation
Integrate the Messaging into your app by including the following in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:messaging:1.0.0-beta-1'
implementation 'com.adobe.marketing.mobile:edge:1.1.0'
implementation 'com.adobe.marketing.mobile:edgeidentity:1.0.0'
implementation 'com.adobe.marketing.mobile:sdk-core:+'
```

Firebase messaging sdk will be required for using FCM
```
implementation 'com.google.firebase:firebase-messaging:<latest-version>'
```

## Documentation
Additional documentation for configuration and sdk usage can be found under the [Documentation](Documentation/README.md) directory.

## Run the Demo app
- Clone or download the project.
- Open the project in Android Studio.
- Click the play button from android studio to run the app.

## Contributing
Contributions are welcomed! Read the [CONTRIBUTING](.github/CONTRIBUTING.md) for more information.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
