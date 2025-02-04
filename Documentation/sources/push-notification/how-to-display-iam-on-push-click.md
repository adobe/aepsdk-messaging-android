# Display an in-message when a push notification is clicked

## Overview
This document describes the steps that allow a designated in-app message to be shown to an end user when they open the app through a push notification click.

## Add a trigger in the In-App Message Campaign

1. In the Journey Optimizer UI, select the Campaign for the in-app message that needs to be shown when a push notification is clicked.
2. Under the **Triggers** section, click on the **Edit triggers** button. Click on **Add Condition** and select the **Manual trigger** from the event dropdown. Click on **Add Condition** again and select **Custom trait** from the trait menu. Enter `adb_iam_id` as the key for the custom trait and an id that uniquely identifies the in-app message as the value. Make a note of this id as it will be used in the next step.

![In-app campaign trigger for Push-to-inapp](./../../../assets/iam-trigger-p2i.png)

3. Click **Done** to save the rule and publish the changes by clicking on **Review to activate**

## Add the In-App Message ID to the Push Notification data

1. In the Journey Optimizer UI, select the Campaign for the Push notification whose click opens the app and shows the in-app message from the previous step.
2. Click **Edit content** to modify the push notification payload. Under **Custom data** section, click on **Add Key/Value Pair**. Enter `adb_iam_id` in the key field and the unique in-app message ID from the previous step in the value field.

![Push campaign custom data for Push-to-inapp](./../../../push-custom-data-p2i.png)

3. Click **Review to activate** to publish the campaign.

## Track push notification interaction

> **Note** : This step can be skipped if your app is [automatically displaying and tracking push notification using AEPMessaging extension](./automatic-handling-and-tracking.md)

After the application is opened by the user by clicking on the push notification, verify push notification interaction is being tracked using [handleNotificationResponse](./manual-handling-and-tracking.md#tracking-push-notification-interactions) API.


