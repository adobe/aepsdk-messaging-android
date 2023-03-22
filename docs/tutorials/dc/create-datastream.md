# Create a Datastream

In order to send data to the Edge Network, the datastream must be configured with the Event schema.

1. Navigate to the Data Collection UI by selecting the nine-dot menu in the top right (**1**), and selecting `Data Collection` (**2**).

| ![Navigating to Data Collection](assets/nav-dc.png?raw=true) |
| :---: |
| **Navigating to Data Collection** |

2. Select **Datastreams** in the left side navigation panel. 

| ![Navigating to Datastreams](assets/datastream-nav.png?raw=true) |
| :---: |
| **Navigating to Datastreams** |

3. Click the **New Datastream** button in the top right.

| ![Add a new Datastream](assets/datastream-new.png?raw=true) |
| :---: |
| **Add a new Datastream** |

4. Give the datastream an identifying name and description (**1**), then pick an Event schema previously created in the [Adobe Experience Platform](https://experience.adobe.com/#/platform) UI section using the dropdown menu (**2**). Finally, hit the **Save** (**3**) button.

| ![Set Datastream Values](assets/datastream-save.png?raw=true) |
| :---: |
| **Set Datastream Values** |

With the datastream set up, data can be directed to its destination by adding services like Adobe Experience Platform.

### Adobe Experience Platform Datastream configuration

1. Click the **Add Service** button.

| ![Add Datastream Service](assets/datastream-add-service.png?raw=true) |
| :---: |
| **Add Datastream Service** |

2. From the **Service (required)** dropdown, select **Adobe Experience Platform** and make sure it is enabled (**1**).
3. From the `Event Dataset` dropdown, select a dataset previously created in the [Adobe Experience Platform](https://experience.adobe.com/#/platform) UI (**2**).
4. Ensure the **Edge Segmentation** and **Adobe Journey Optimizer** boxes (**3**) are checked.
4. Hit the **Save** (**4**) button.

| ![Add Experience Platform to Datastream](assets/datastream-service-save.png?raw=true) |
| :---: |
| **Add Experience Platform to Datastream** |
