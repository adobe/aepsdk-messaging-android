#  AEPMessage Documentation


### Installation

- [Installing AEPMesssaging extension](./sources/getting-started.md)
- [Configure Adobe Data Collection and Adobe Experience Platform](./sources/edge-and-launch-configuration.md)
- [Test app setup](./sources/testapp-setup.md)

### Push messaging

- Prerequisites
  - Enable push notifications in your app by [adding the Firebase dependency.](https://firebase.google.com/docs/cloud-messaging/android/client)
- Developer Documentation
  - [API usage](./sources/api-usage.md#Push Messaging APIs)
  - [PushTrackingStatus](./sources/enum-public-classes/enum-push-tracking-status.md)
  - [MessagingPushPayload](./sources/enum-public-classes/messaging-push-payload.md)
- Guides
  - [Handling the received push payload](./sources/enum-public-classes/messaging-push-payload.md)
  - [Manual display and tracking of push notification](./sources/push-notification/manual-handling-and-tracking.md)
  - [Automatic display and tracking of push notification](./sources/push-notification/automatic-handling-and-tracking.md)

### In-app Messaging

- Developer Documentation
  - [API usage](./sources/api-usage.md#In-App Messaging APIs)
  - [Message](./sources/enum-public-classes/class-message.md)
- Guides
  - [Programmatically control the display of in-app messages](./sources/in-app-messaging/how-to-presentation-delegate.md)
  - [Call native code from the Javascript of an in-app message](./sources/in-app-messaging/how-to-call-native-from-javascript.md)
  - [Execute Javascript code in an in-app message from native code](./sources/in-app-messaging/how-to-call-javascript-from-native.md)
  - [Handle URL clicks from an in-app message](./sources/in-app-messaging/how-to-handle-url-clicks.md)

### Code-based experiences and content cards

- Developer Documentation
  - [API usage](./sources/api-usage.md#Message Feed and Code-based experiences APIs)
  - Public classes
    - [ContentCard](./sources/propositions/content-card.md)
    - [Proposition](./sources/propositions/proposition.md)
    - [PropositionItem](./sources/propositions/proposition-item.md)
    - [Surface](./sources/propositions/surface.md)
    - Schema classes
      - [ContentCardSchemaData](./sources/propositions/schemas/content-card-schema-data.md)
      - [HtmlContentSchemaData](./sources/propositions/schemas/html-content-schema-data.md)
      - [InAppSchemaData](./sources/propositions/schemas/inapp-schema-data.md)
      - [JsonContentSchemaData](./sources/propositions/schemas/json-content-schema-data.md)

### Common public classes, methods, and enums

- [MessagingEdgeEventType](./sources/enum-public-classes/enum-messaging-edge-event-type.md)
