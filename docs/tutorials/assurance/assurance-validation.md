# Assurance Validation

The Messaging extension, and Adobe SDK extensions in general, integrate with [Assurance](https://experience.adobe.com/assurance) which helps to:

* Inspect and validate SDK events
* Verify extension versions and configuration
* View SDK logs, device information 
* Simulate workflows
* Much more!

### Create Assurance Session

Follow the steps below to create an Assurance session:

1. Navigate to the Data Collection UI by selecting the nine-dot menu in the top right (**1**), and selecting `Data Collection` (**2**).

| ![Navigating to Data Collection](assets/nav-dc.png?raw=true) |
| :---: |
| **Navigating to Data Collection** |

2. Select **Assurance** in the left side navigation panel (**1**) then click the **Create Session** (**2**) button in the top right.

| ![Create Assurance Session](assets/assurance-create-session.png?raw=true) |
| :---: |
| **Create Assurance Session** |

3. In the **Create New Session** dialog, click on **Start**.

4. Next, provide the **Session Name** as `IAM Tutorial Session` and the **Base URL** as `iamTutorial://`. Click the **Next** button.

5. A new Assurance session is created and the provided QR Code or Link can be used to connect to the session. Select **Copy Link** and 
click on the copy icon (**1**) to select the link. Make a note of the **PIN**. Click **Done**.

| ![Create Assurance Session - Start](assets/assurance-start-session.png?raw=true) | ![Create Assurance Session - Provide Details](assets/assurance-session-info.png?raw=true) | ![Assurance Session - QR Code](assets/assurance-copy-link-pin.png?raw=true) |
| :---: | :---: | :---: |
| **Create Assurance Session - Start** | **Create Assurance Session - Provide Details** | **Assurance Session - Connecting** |

>[!NOTE]
> For more details on `BASE URL`, see article [Defining custom URL scheme for your app](https://developer.apple.com/documentation/xcode/defining-a-custom-url-scheme-for-your-app).

### Connecting the InappTutorialApp to Assurance

1. On the iOS Simulator, click on the **Safari App** icon. 
    * If you are not on the iOS Simulator's _Home_ screen, get there by pressing **(⌘ + Shift + H)**.

2. Right-click in the address bar and select **Paste and Go**.

3. A dialog box should appear prompting you to open the page in the **InappTutorialApp**. Click **Open**.
    * If this dialog doesn't appear, try copying the Assurance link again from Step 5 above.

| ![Launch Safari](assets/assurance-safari.png?raw=true) | ![Paste Deeplink](assets/assurance-safari-paste.png?raw=true) | ![Open in Tutorial App](assets/assurance-safari-deeplink.png?raw=true) |
| :---: | :---: | :---: |
| **Launch Safari** | **Paste Deeplink** | **Open in Tutorial App** |

4. When the app launches, a screen prompting you to enter a connection PIN will appear. Type in the 4-digit PIN from the Assurance UI, and hit **Connect** on the PIN screen.

| ![Assurance Session - Enter Pin](assets/assurance-app-enter-pin.png?raw=true) |
| :---: |
| **Assurance Session - Enter Pin** |

5. The app should now be connected to assurance, as shown by **1 Client Connected** in the header.

| ![Assurance Session Connected](assets/assurance-app-connected.png?raw=true) |
| :---: |
| **Assurance Session Connected** |

### Using the In-App Messaging plugin

Now that the app is connected to Assurance, we can see events beginning to flow in from the SDK. Since we are specifically working on in-app messages, we can use a more customized view to help us by installing the **In-App Messaging** plugin.

1. In the bottom left of the Assurance UI, click the **Configure** button. 

2. Click the **+** button next to **In-App Messaging** (**1**) when the list of available plugins appears. Then hit the **Save** button (**2**).

| ![Install In-App Messaging plugin](assets/assurance-install-messaging-plugin.png?raw=true) |
| :---: |
| **Install In-App Messaging plugin** |

3. Click on **In-App Messaging** in the left side navigation panel (**1**). 

This plugin has three tabs across the top (**2**) that provide three new, in-app messaging specific views.

| ![In-App Messaging plugin](assets/assurance-messaging-plugin.png?raw=true) |
| :---: |
| **In-App Messaging plugin** |

##### Messages on Device

4. Select the message from **Message** dropdown (**1**).

5. Click the **Simulate on Device** button (**2**) to force the message to be shown on the device.

| ![Simulate on device](assets/assurance-preview-on-device.png?raw=true) |
| :---: |
| **Simulate on device** |

##### Event List

6. Click the **Event List** tab.

7. The list now shows all in-app messaging related events, including:
    * Requests for new messages from the remote server
    * Messages received from the remote server
    * Messages being displayed and dismissed
    * Custom user interactions with the message

| ![Simulate on device](assets/assurance-messaging-event-list.png?raw=true) |
| :---: |
| **Simulate on device** |

> [!NOTE]
> Assurance sessions are deleted after a period of 30 days.

