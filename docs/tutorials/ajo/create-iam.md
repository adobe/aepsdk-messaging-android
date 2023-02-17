# Create an in-app message

 In this section we will create an in-app message in **Adobe Journey Optimizer**. This consists of creating the message, configuring the audience, and creating the message content.

### Create the in-app message campaign

1. Go to the [Adobe Experience Platform](https://experience.adobe.com/#/platform), using your Adobe ID credentials to log in if prompted.

2. Navigate to the **Journey Optimizer** UI by selecting the nine-dot menu in the top right (**1**), and selecting `Journey Optimizer` (**2**).

| ![Navigating to Journey Optimizer](assets/ajo-nav.png?raw=true) |
| :---: |
| **Navigating to Journey Optimizer** |

3. Select **Campaigns** (**1**) in the left navigation window.

4. Click the **Create campaign** button (**2**) on the upper right of the screen.

| ![Create a new Campaign](assets/ajo-create-campaign.png?raw=true) |
| :---: |
| **Create a new Campaign** |

### Configure the audience and the message

1. Check the box for `In-app message` and select the app surface you created earlier in this tutorial.

| ![Create an in-app message](assets/ajo-create-iam.png?raw=true) |
| :---: |
| **Create an in-app message** |

> [!NOTE]
> For this example, we will show the message to everyone, every time an SDK call of `MobileCore.trackAction("50off", nil)` is made.

2. Give your message a name (**1**).  This will help you identify your message from the list of campaigns.

3. Ensure the `Show every time` radio button is selected (**2**).

4. Configure the client side event trigger (**3**):

    - In the **Mobile app trigger** dropdown, select `Custom Track Action Event`.

    - In dropdown on the right, select `Equals`.

    - In the **Value** text field, enter `50off`.

5. Hit the **Edit content** button (**4**) to begin creating the message content.

| ![Configure in-app message audience](assets/ajo-iam-triggers.png?raw=true) |
| :---: |
| **Configure in-app message audience** |

6. In the **Message layout** section (**1**), click the **Modal** button.

7. In the **Media** section (**2**), paste into the **Media URL** field the following image URL: `https://t4.ftcdn.net/jpg/00/63/83/29/360_F_63832907_SA64nRfoIU8qaPKDkcYT7Ax2T0eVFJDY.jpg`

8. In the **Content** section (**3**), enter some values in for the **Header** and **Body** of the message. For this tutorial, we will thank our loyal customer and offer a discount on their next purchase.

9. In the **Buttons** section (**4**), change the text of button.

10. Hit the **<-** button (**5**) to go back to the previous screen.

| ![Configure in-app message content](assets/ajo-iam-content.png?raw=true) |
| :---: |
| **Configure in-app message content** |

11. Hit the **Review to activate** button.

| ![Review to activate](assets/ajo-iam-review-to-activate.png?raw=true) |
| :---: |
| **Review to activate** |

12. This page shows a summary of all the configuration for the message.  Hit the **Activate** button in the top right.

| ![Activate in-app message](assets/ajo-iam-review.png?raw=true) |
| :---: |
| **Activate in-app message** |

13. In the Campaigns list, your new message should be visible.  It will take some time to activate, but eventually you should see a **Status** of `Live` for your message.

| ![In-app message live in list of Campaigns](assets/ajo-iam-activated.png?raw=true) |
| :---: |
| **In-app message live in list of Campaigns** |

In the next section we will review the installation instructions for the SDK.