# Create a Schema
Before any app changes, some configuration items on the Adobe Experience Platform (AEP) side need to be set up. First, create an XDM schema (the format for data that the Edge Network uses).
 
At a high level, a schema is a definition for the structure of your data; what properties you are expecting, what format they should be in, and checks for the actual values coming in.  

1. Go to the [Adobe Experience Platform](https://experience.adobe.com/#/platform), using your Adobe ID credentials to log in if prompted.

2. Navigate to the Experience Platform UI by selecting the nine-dot menu in the top right (**1**), and selecting `Experience Platform` (**2**)

| ![Navigating to Experience Platform](../../assets/aep-experience-platform.png?raw=true) |
| :---: |
| **Navigating to Experience Platform** |

3. Select **Schemas** (**1**) in the left navigation window

| ![Navigating to Schemas in Data Collection](../../assets/experience-platform-schemas.png?raw=true) |
| :---: |
| **Navigating to Schemas Data Collection** |

4. In the schemas view, select the **+ Create schema** button (**1**) in the top right, then select `XDM ExperienceEvent` (**2**)

| ![Creating new XDM ExperienceEvent schema](../../assets/data-collection-schemas.png?raw=true) |
| :---: |
| **Creating new XDM ExperienceEvent schema** |

Once in the new schema creation view, notice the schema class is `XDM ExperienceEvent` (**1**); schemas adhere to specific class types which just means that they have some predefined properties and behaviors within the Edge platform. In this case, `XDM ExperienceEvent` creates the base properties you see in the `Structure` section that help define some baseline data for each Experience Event. 

5. Give the new schema a name and description (**2**) to help identify it.
6. Select the `+ Add` button (**3**) next to the `Field groups` section under `Composition`.

| ![Initial schema creation view](../../assets/schema-creation.png?raw=true) |
| :---: |
| **Initial schema creation view** |

Add the following Adobe defined field group to the schema:  
- Experience Event - Proposition Interactions 

Use the search box (**1**) to look up the name (**2**) of the field group required for this section. Note the owner of the schemas should be **Adobe** (**3**).

Verify that the **Experience Event - Proposition Interactions** field group is present in the right side info panel (**4**), then select **Add field groups** (**5**).

| ![Add field groups required for Target](../../assets/schema-field-group-selected.png?raw=true) |
| :---: |
| **Add field groups required for Target** |

Verify that the **Experience Event - Proposition Interactions** field group present under the **Field groups** section (**1**) and the properties associated with this field group is present under the **Structure** section (**2**), then select **Save** (**3**).

| ![Schema with field groups required for Target](../../assets/schema-with-field-groups.png?raw=true) |
| :---: |
| **Schema with field groups required for Target** |
