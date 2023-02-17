# Create a Target Activity

Activities in Adobe Target enable you to personalize offer content for specific audiences. For more details, see [Adobe Target - Activities](https://experienceleague.adobe.com/docs/target/using/activities/activities.html?lang=en).

Follow the steps below to set up an Experience Targeting activity in Adobe Target:

1. Navigate to **Activities (1)**, then click on **Create Activity (2)** and select **Experience Targeting (3)** from the activity type dropdown.

| ![Target Activities](../../assets/target-activities.png?raw=true) |
| :---: |
| **Target Activities** |

2. In the modal dialog that appears, select **Mobile App (1)** channel, specify the previously created [Target Property](https://opensource.adobe.com/aepsdk-optimize-ios/#/tutorials/setup/setup-target-property) by selecting it from the **Choose Property** dropdown (2). Click **Next (3)**.

| ![Create Target Activity](../../assets/target-activity-create.png?raw=true) |
| :---: |
| **Create Target Activity** |

3. Click on **rename (1)** in the top bar to provide an activity name as `Optimize Tutorial Activity` for the Untitled Activity. Specify the **Location (2)** name as `optimize-tutorial-loc`, where personalized content will be rendered in your mobile app. Then click on **+ Add Experience Targeting (3)** to create experiences targeted to different audiences.

| ![Specify Target Activity Details](../../assets/target-activity-create-details.png?raw=true) |
| :---: |
| **Specify Target Activity Details** |

4. To target an experience to an audience, audience selection must happen before experience can be added. An existing audience can be selected from the Audience Library, which contains previously created audiences as well as common pre-built Target audiences, or a new one can be created. Click on **Create audience (1)** to create a new audience. 

| ![Experience B - Add Audiences](../../assets/target-experienceB-audience-add.png?raw=true) |
| :---: |
| **Experience B - Add Audiences** |

5. In the **Create Audience** dialog, select **Custom (1)** under **Attributes**, then drag and drop it to the central pane to set up an Audience rule. Specify the rule using mobile Lifecycle parameter `a.Launches`, sent as mbox parameter to Adobe Target, by selecting `Is greater than` operator and setting Values `1` as shown in (2). Select the option to save the audience to **This activity only (3)**. Provide a **Name (4)** `Lifecycle Launches Gt 1`. Click **Done (5)**.

| ![Experience B - Create Audience](../../assets/target-experienceB-audience-create.png?raw=true) |
| :---: |
| **Experience B - Create Audience** |

<details>
<summary>
TIP: Create audience when a parameter doesn't already exist in Adobe Target.
</summary>
<p>
If mobile Lifecycle metrics are not previously sent as mbox parameters, Target will provide a pop-up to <b>Create parameter a.Launches (1)</b> when specifying the rule. Click on the pop-up. 
</p>

| ![Experience B - Create Audience Parameter](../../assets/target-experience-audience-parameter-create.png?raw=true) |
| :---: |
| **Experience B - Create Audience Parameter** |

<p>
Target will then provide a confirmation dialog to <b>Create new parameter</b>. Click <b>Save (1)</b>.
</p>

| ![Experience B - Create Audience Parameter Confirm](../../assets/target-experience-audience-parameter-save.png?raw=true) |
| :---: |
| **Experience B - Create Audience Parameter Confirm** | 
</details>

6. Once the **Add audiences** dialog opens up again, select the audience created previously by clicking on the checkbox (1) present in the corresponding row. Then click on **Assign Audience (2)**.


| ![Experience B - Assign Audience](../../assets/target-experienceB-audience-assign.png?raw=true) |
| :---: |
| **Experience B - Assign Audience** |

7. In the Target activity, verify **Experience B** is created (1) and audience is assigned by clicking on the info icon (2).

| ![Experience B](../../assets/target-experienceB-create-verify.png?raw=true) |
| :---: |
| **Experience B** |

8. Select **Experience A** and click on three vertical ellipsis (1), then select **Change Audience (2)** from the pop-up menu. 

| ![Experience A - Change Audience](../../assets/target-experienceA-audience-change.png?raw=true) |
| :---: |
| **Experience A - Change Audience** |

9. The Audience Library shows up again (similar to step 4). Click on **Create audience (1)** to create a new audience.

| ![Experience A - Add Audiences](../../assets/target-experienceA-audience-add.png?raw=true) |
| :---: |
| **Experience A - Add Audiences** |

10. In the **Create Audience** dialog that follows (similar to step 5), select **Custom (1)** under **Attributes**, then drag and drop it to the central pane to set up an Audience rule. Specify the rule using mobile Lifecycle parameter `a.Launches`, sent as mbox parameter to Adobe Target, by selecting `Equals` operator and setting Values `1` as shown in (2). Select the option to save the audience to **This activity only (3)**. Provide a **Name (4)** `Lifecycle Launches Eq 1`. Click **Done (5)**.

| ![Experience A - Create Audience](../../assets/target-experienceA-audience-create.png?raw=true) |
| :---: |
| **Experience A - Create Audience** |

11. Once the **Add audiences** dialog opens up again, select the audience created previously by clicking on the checkbox (1) present in the corresponding row. Then click on **Assign Audience (2)**.

| ![Experience A - Assign Audience](../../assets/target-experienceA-audience-assign.png?raw=true) |
| :---: |
| **Experience A - Assign Audience** |

12. In the Target activity, verify **Experience A** audience is assigned by clicking on the info icon (1).

| ![Experience A](../../assets/target-experienceA-verify.png?raw=true) |
| :---: |
| **Experience A** |

13. With the **Experience A** selected, click on the arrow (1) next to `Default Content` under <small>CONTENT</small> section and select **Create HTML Offer (2)** from the dropdown.

| ![Experience A - Create Offer](../../assets/target-experienceA-content-create.png?raw=true) |
| :---: |
| **Experience A - Create Offer** |

14. Provide **HTML Content (1)** in the text area. See example below:

```html
<html><body><img src="https://d14dq8eoa1si34.cloudfront.net/2a6ef2f0-1167-11eb-88c6-b512a5ef09a7/urn:aaid:aem:649f9f94-f2ed-46c5-8d07-88768d3fe5a8/oak:1.0::ci:ff5f812bfecc17440a2b5daeb83ea2f5/bc7a6fc4-0daa-303e-b9a7-75cc6c02b734" style="display: block;margin-left: auto;margin-right: auto;width: 80%;"/></body></html>
```

| ![Experience A - Offer HTML Content](../../assets/target-experienceA-content.png?raw=true) |
| :---: |
| **Experience A - Offer HTML Content** |

15. Next select **Experience B**, click on the arrow (1) next to `Default Content` under <small>CONTENT</small> section and select **Create HTML Offer (2)** from the dropdown.

| ![Experience A - Create Offer](../../assets/target-experienceB-content-create.png?raw=true) |
| :---: |
| **Experience A - Create Offer** |

16. Provide **HTML Content (1)** in the text area. See example below:

```html
<html><body><img src="https://d14dq8eoa1si34.cloudfront.net/2a6ef2f0-1167-11eb-88c6-b512a5ef09a7/urn:aaid:aem:2d0a92da-92ea-4179-9336-ac0311f950e6/oak:1.0::ci:fab6e76b035130f5962cb46c90656b4a/7d30ca37-6649-34e6-ad4d-54741a407354" style="display: block;margin-left: auto;margin-right: auto;width: 80%;"/></body></html>
```

| ![Experience B - Offer HTML Content](../../assets/target-experienceB-content.png?raw=true) |
| :---: |
| **Experience B - Offer HTML Content** |

Once done, click **Next (2)**

17. In the **Targeting** section, verify the audiences and the experiences (1). Click **Next (2)**.

| ![Targeting - Verify](../../assets/target-targeting-verify.png?raw=true) |
| :---: |
| **Verify Targeting** |

18. Specify the **Goals & Settings** for the activity. Keep the default **Activity Settings** - Priority and Duration. Under **Reporting Settings**, keep the **Reporting Source** as **Adobe Target (1)**. Specify the **Goal Metric** by clicking on the down arrow (2) to **Select success metric**. Select **Conversion (3)** from the dropdown menu. 

| ![Goal Success Metric - Conversion](../../assets/target-goal-success-conversion.png?raw=true) |
| :---: |
| **Goal Success Metric - Conversion** |

19. Specify the **Goal Metric** action by clicking on the down arrow (1) to **Select action**. Select **Clicked on mbox (2)** from the dropdown menu.

| ![Goal Action - Clicked on mbox](../../assets/target-goal-action-clicked.png?raw=true) |
| :---: |
| **Goal Action - Clicked on mbox** |

20. Speciy the mbox name (1) `optimize-tutorial-loc` on which goal action should be taken. Click on **Save & Close (2)** to save the activity. 

| ![Save & Close Target Activity](../../assets/target-activity-save.png?raw=true) |
| :---: |
| **Save & Close Target Activity** |

21. Verify Target provides a confirmation banner indicating that the activity is ready to <small>ACTIVATE</small>.

| ![Target Activity Save Confirmation](../../assets/target-activity-save-confirm.png?raw=true) |
| :---: |
| **Target Activity Save Confirmation** |

22. Verify Activity Overview, then click the down arrow (1) next to **Inactive** and select **Activate (2)** from the dropdown menu.

| ![Activate Target Activity](../../assets/target-activity-activate.png?raw=true) |
| :---: |
| **Activate Target Activity** |

23. Verify Target provides a confirmation banner indicating that the activity is <small>LIVE</small>.

| ![Target Activity Live Confirmation](../../assets/target-activity-activate-confirm.png?raw=true) |
| :---: |
| **Target Activity Live Confirmation** |

24. The activated Target Activity's mbox location `optimize-tutorial-loc` is now ready to be used in your mobile application for experience targeting. 
