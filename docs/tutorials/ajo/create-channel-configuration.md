# Create a Channel Configuration

In order for you to properly select the destination app of an in-app message in Adobe Journey Optimizer, you will need to create a **Channel configuration** for your application.

1. Go to the [Adobe Experience Platform](https://experience.adobe.com/#/platform), using your Adobe ID credentials to log in if prompted.

2. Navigate to the **Journey Optimizer** UI by selecting the nine-dot menu in the top right (**1**), and selecting `Journey Optimizer` (**2**).

| ![Navigating to Journey Optimizer](assets/ajo-nav.png?raw=true) |
| :---: |
| **Navigating to Journey Optimizer** |

3. Select **Channels** in the left side navigation panel. 

| ![Navigating to Channels](assets/channel-nav.png?raw=true) |
| :---: |
| **Navigating to Channels** |

4. Click the **Create channel configuration** button in the top right.

| ![Create new channel configuration](assets/channel-new.png?raw=true) |
| :---: |
| **Create new channel configuration** |

5. Give the channel configuration an identifying name and description (**1**). 
6. From the **Select channel** drop down, select **In-app messaging** (**2**).
7. Under the In-app messaging settings section, select the platform you're targeting. For iOS, check **iOS** (**3**) and enter the tutorial app's bundle identifier in the **App id** field under **iOS** (**4**). For Android, check **Android** (**5**) and enter the tutorial app's app id in the **App id** field under **Android** (**6**).
8. Hit the **Submit** (**7**) button.

> [!NOTE]
> The _tutorial app_ referenced in this section is an app created specifically for this tutorial. At this point in the tutorial it has not yet been introduced - it will be covered in a future section.

| ![Set Channel Configuration values](assets/channel-save.png?raw=true) |
| :---: |
| **Set channel configuration values** |

With the Channel configuration created, we can now move on to creating and authoring the in-app message.
