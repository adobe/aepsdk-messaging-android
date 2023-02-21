# Using the Tutorial App to trigger the in-app message

### Getting Started

Follow the steps below to download the InappTutorialApp from the [Messaging GitHub repository](https://github.com/adobe/aepsdk-messaging-android).

1. Navigate to the GitHub repository using the URL https://github.com/adobe/aepsdk-messaging-android/tree/messaging-tutorials.

2. Click on **Code (1)** then select **Download ZIP (2)** from the pop-up dialog.

| ![Messaging Extension Code](assets/messaging-github-code.png?raw=true) |
| :---: |
| **Messaging Extension Code** |

> [!NOTE]
> Steps 3-6 in this section include commands you can run from your favorite Terminal app.  

3. Copy the `aepsdk-messaging-android-messaging-tutorials.zip` file from your `Downloads` directory to another appropriate location. For example, your home directory

```
mv ~/Downloads/aepsdk-messaging-android-messaging-tutorials.zip ~/
```

4. Unzip the file in the target location.

```
cd ~/
unzip aepsdk-messaging-android-messaging-tutorials.zip
```

5. Change directory to the `tutorialApp`

```
cd aepsdk-messaging-android-messaging-tutorials/docs/tutorials/tutorialApp
```

6. Open the gradle file `build.grade` in Android Studio.

### Install AEPMessaging SDK Extension in your mobile application

Follow the steps in [Install SDK Extensions guide](../getting-started/install-sdk-extensions.md) to install the AEPMessaging SDK extension and its dependencies in your mobile application.

### Initialize the mobile SDK

Follow the steps in [Initialize SDK guide](../getting-started/init-sdk.md) to initialize the Experience Platform mobile SDK by registering the SDK extensions with `Mobile Core`.

For this tutorial, initlization code is already implemented in `tutorialApp`.

### Run the mobile application

Follow the steps below to run the `tutorialApp` :

1. Select the mobile app target **tutorialApp** and destination of your connected Android device. Click on Play icon (**1**) to launch the app.

| ![Run Mobile App](assets/messaging-app-run.png?raw=true) |
| :---: |
| **Run Mobile App** |

2. The iOS simulator will launch and you should see the app running. Enter **50off** in the text input field then click the **TRIGGER IAM** button to show the in-app message.

|![Running App](assets/messaging-app-simulator.png?raw=true) | ![In-app message showing](assets/messaging-app-showing.png?raw=true) |
| :---------: | :---------: |
| **Running App** | **In-app message showing** |