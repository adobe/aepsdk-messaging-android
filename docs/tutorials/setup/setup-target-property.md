# Set up a Target property

Target properties can be set up and associated with Workspaces in Adobe Target to restrict activities, audiences and offers access to a specific set of users. For more details, see [Set up Properties in Adobe Target](https://experienceleague.adobe.com/docs/target-learn/tutorials/administration/set-up-properties.html?lang=en).

Follow the steps below to set up a Target property in Adobe Target:

1. Navigate to **Administration (1) > Properties (2)** to display the list of properties.
2. Click on **Create Property (3)**

| ![Target Properties](../../assets/target-properties.png?raw=true) |
| :---: |
| **Target Properties** |

3. In the **Create Property** modal dialog, specify the **Property Name** (required), **Description** (optional), set **Channel** to **Mobile**, then click **Save**.

| ![Create Target Property](../../assets/target-property-create.png?raw=true) |
| :---: |
| **Create Target Property** |

4. Verify that the newly created Target property (1) appears in the **Properties** list.


| ![Verify Target Property](../../assets/target-property-verify.png?raw=true) |
| :---: |
| **Verify Target Property** |

5. Click on **Code (1)** icon for your created property in the list. A popup will appear displaying the Implementation Code for your property. Copy the **at_property** token value (2).

| ![Copy Target Property](../../assets/target-property-copy.png?raw=true) |
| :---: |
| **Copy Target Property** |

> [!NOTE]
> You will need to specify this property in your datatstream configuration for Adobe Target (Property Token field). For more details, see [Adobe Target Datastream configuration](https://opensource.adobe.com/aepsdk-optimize-ios/#/tutorials/setup/create-datastream?id=adobe-target-datastream-configuration)
