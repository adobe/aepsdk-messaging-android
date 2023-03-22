# Create a Tag Property

In this section we will create a mobile property which contains the configuration settings for all AEP SDK extensions in your app.

### Create the Tag Property

1. Navigate to the Data Collection UI by selecting the nine-dot menu in the top right (**1**), and selecting `Data Collection` (**2**).

| ![Navigating to Data Collection](assets/nav-dc.png?raw=true) |
| :---: |
| **Navigating to Data Collection** |

2. Select **Tags** in the left side navigation panel (**1**) then click the **New Property** (**2**) button in the top right.

| ![Create new Tag Property](assets/tag-new.png?raw=true) |
| :---: |
| **Create new Tag Property** |

3. Provide a name for the tag (**1**). 
4. Ensure `Mobile` is selected for the **Platform** (**2**). 
5. Ensure `Opted In` is selected for **Privacy** (**3**).
6. Click the **Save** button (**4**).

| ![Enter Tag Property details](assets/tag-name.png?raw=true) |
| :---: |
| **Enter Tag Property details** |

### Configure extensions

1. Select your tag from the list (**1**). If it's helpful, use the search field to make finding your tag easier (**2**). 

| ![Select Tag Property](assets/tag-select.png?raw=true) |
| :---: |
| **Select Tag Property** |

2. Select **Extensions** in the left side navigation panel.

| ![Navigate to Extensions](assets/tag-nav-extensions.png?raw=true) |
| :---: |
| **Navigate to Extensions** |

3. Click the **Catalog** tab near the tag name and scroll down until you see the **Identity** extension. Click the **Install** button.

> [!NOTE]
> No additional configuration is needed for the **Identity** extension.

| ![Install Identity extension](assets/tag-extensions-identity.png?raw=true) |
| :---: |
| **Install Identity extension** |

4. Click the **Catalog** tab near the tag name and scroll down until you see the **Adobe Experience Platform Edge Network** extension. Click the **Install** button.

| ![Install Adobe Experience Platform Edge Network extension](assets/tag-extensions-aep.png?raw=true) |
| :---: |
| **Install Adobe Experience Platform Edge Network extension** |

> [!Tip]
> When configuring the Edge extension, we will be using the Datastream created in part one (1) of this tutorial.

5. For each of the **Production**, **Staging**, and **Development** environments (**1**), ensure the sandbox matches the sandbox in which you created your Datastream. and select your Datastream from the dropdown.

6. In the **Edge Network domain** text field, enter your domain (**2**). If you don't have a custom domain, enter `edge.adobedc.net`

| ![Adobe Experience Platform Edge Network extension configuration](assets/tag-aep-details.png?raw=true) |
| :---: |
| **Adobe Experience Platform Edge Network extension configuration** |

7. Click the **Catalog** tab near the tag name and scroll down until you see the **Adobe Journey Optimizer** extension. Click the **Install** button.

| ![Install Adobe Journey Optimizer extension](assets/tag-extensions-ajo.png?raw=true) |
| :---: |
| **Install Adobe Journey Optimizer extension** |

8. In the **Event Dataset** dropdown, select the **AJO Push Tracking Experience Event Dataset** Dataset.

> [!NOTE]
> This is the correct datastream for configuring Push Messaging in AJO, which will not be covered in this tutorial.

| ![Adobe Journey Optimizer extension configuration](assets/tag-ajo-details.png?raw=true) |
| :---: |
| **Adobe Journey Optimizer extension configuration** |


### Publish the Tag Property

1. In the top right of the screen, click the dropdown that says **Select a working library** then click **Add library...**.

| ![Create a Library](assets/tag-library-create.png?raw=true) |
| :---: |
| **Create a Library** |

2. Name your new library (**1**).
3. Select the **Development** environment (**2**).
4. Click the **Add All Changed Resources** button at the bottom (**3**).
5. Hit the **Save & Build to Development** button in the top right (**4**).

| ![Configure Library](assets/tag-library-details.png?raw=true) |
| :---: |
| **Configure Library** |

6. When the library has completed building, the dot next to its name will turn green.

| ![A successfully built Library](assets/tag-library-build.png?raw=true) |
| :---: |
| **A successfully built Library** |

7. Select **Environments** in the left side navigation panel (**1**).
8. In the far right column of your **Development** environment, click the button in the **Install** column (**2**).

| ![Navigate to Environments](assets/tag-environments.png?raw=true) |
| :---: |
| **Navigate to Environments** |

9. This screen shows installation instructions for your app based on the Tag Property's configuration. It includes:
  - Your **Environment File ID** (**1**) which helps the SDK locate your remote configuration.
  - Dependencies for your **Gradle** file (**2**).
  - Initialization code for your app (**3**).

> [!NOTE]
> For this tutorial, the provided app will already have these values in it. This step in the tutorial was for reference only.

| ![View install instructions](assets/tag-install-instructions-android.png?raw=true) |
| :----------------------------------------------------------: |
|                **View install instructions**                 |

For the next part of the tutorial, we will show how to author an in-app message in **Adobe Journey Optimizer**.