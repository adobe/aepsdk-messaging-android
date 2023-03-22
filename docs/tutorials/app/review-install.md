# Review SDK installation instructions

In this section is for reference only.  It covers how to get access to the **AEPMessaging** SDK extension that supports in-app messaging in AJO. 

### Install the AEPMessaging extension

To install the **AEPMessaging** SDK, add the following in your App's gradle file:

| ![Installing the Messaging extension](assets/docs-install.png?raw=true) |
| :---: |
| **Installing the Messaging extension** |

### Register the AEPMessaging extension

To initialize the **AEPMessaging** extension in your app, `import` it and call `Messaging.registerExtension()`.

> [!NOTE]
> When the AEPMessaging extension is registered by the Adobe AEP SDK, it will automatically attempt to fetch in-app messages for the device based on the app configuration.

| ![Registering the Messaging extension](assets/docs-register.png?raw=true) |
| :---: |
| **Registering the Messaging extension** |
