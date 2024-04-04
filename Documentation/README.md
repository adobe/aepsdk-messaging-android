#  AEPMessage Documentation

### Prerequisites
- Push Messaging
  - Enable push notifications in your app by [adding the Firebase dependency.](https://firebase.google.com/docs/cloud-messaging/android/client)
- Push and In-app Messaging
  - [Configure Adobe Data Collection and Adobe Experience Platform](./sources/edge-and-launch-configuration.md)


### Getting started with AEPMessaging Extension

- [Getting started](./sources/getting-started.md)
- [Test app setup](./sources/testapp-setup.md)
- [API usage](./sources/api-usage.md)
- [Handling the received push payload](./sources/messaging-push-payload.md)

### Push Notification
- [Manual display and tracking of push notification](./sources/push-notification/manual-handling-and-tracking.md)
(or)
- [Automatic display and tracking of push notification](./sources/push-notification/automatic-handling-and-tracking.md)

### Guides and How-tos

- [Programmatically control the display of in-app messages](./sources/in-app-messaging/how-to-presentation-delegate.md)
- [Call native code from the Javascript of an in-app message](./sources/in-app-messaging/how-to-call-native-from-javascript.md)
- [Execute Javascript code in an in-app message from native code](./sources/in-app-messaging/how-to-call-javascript-from-native.md)
- [Handle URL clicks from an in-app message](./sources/in-app-messaging/how-to-handle-url-clicks.md)

### Other public classes, methods, and enums (TODO: to be updated with Messaging 3.0 classes)

- [Message](./sources/enum-public-classes/class-message.md)
- [MessagingEdgeEventType](./sources/enum-public-classes/enum-messaging-edge-event-type.md)
- [Message Feed and Code Based Experiences](./sources/enum-public-classes/message-feed-and-code-based-experiences.md)
- [MessagingPushPayload](./sources/enum-public-classes/messaging-push-payload.md)
- [PushTrackingStatus](./sources/enum-public-classes/enum-push-tracking-status.md)
