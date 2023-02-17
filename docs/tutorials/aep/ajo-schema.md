# Review in-app messaging schema

Before starting configuration of your app, we will take a moment to review some auto-provisioned data structures in Adobe Experience Platform (AEP). 
 
At a high level, a schema is a definition for the structure of your data; what properties you are expecting, what format they should be in, and checks for the actual values coming in.  

1. Go to the [Adobe Experience Platform](https://experience.adobe.com/#/platform), using your Adobe ID credentials to log in if prompted.

2. Navigate to the Experience Platform UI by selecting the nine-dot menu in the top right (**1**), and selecting `Experience Platform` (**2**).

| ![Navigating to Experience Platform](assets/aep-nav.png?raw=true) |
| :---: |
| **Navigating to Experience Platform** |

3. Select **Schemas** (**1**) in the left navigation window.

| ![Navigating to Schemas in Experience Platform](assets/aep-schemas.png?raw=true) |
| :---: |
| **Navigating to Schemas Experience Platform** |

4. In the schemas view, type the following text into the **search box** (**1**) in the top left: 

    `AJO Inbound`

5. Click on the `AJO Inbound Experience Event Schema` link from the list. (**2**)

> [!NOTE]
> The **AJO Inbound Experience Event Schema** is automatically generated for you when Adobe Journey Optimizer is provisioned for your company.

| ![Selecting AJO Inbound Experience Event Schema](assets/aep-schema-select.png?raw=true) |
| :---: |
| **Selecting AJO Inbound Experience Event Schema** |

6. Expand the tree starting with **_experience** and notice the two parts which are important for in-app messaging reporting. 
    - `propositionAction.label` (**1**) will hold the custom string for your interact events
    - The values `dismiss`, `display`, `interact`, and `trigger` will be present in `propositionEventType` (**2**) for each of their respective events.

| ![AJO Inbound Experience Event Schema Details](assets/aep-schema-details.png?raw=true) |
| :---: |
| **AJO Inbound Experience Event Schema Details** |
