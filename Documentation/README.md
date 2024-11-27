#  AEPMessaging Documentation


### Installation

- [Installing AEPMesssaging extension](./sources/getting-started.md)
- [Configure Adobe Data Collection and Adobe Experience Platform](./sources/edge-and-launch-configuration.md)
- [Test app setup](./sources/testapp-setup.md)

### Push messaging

- Prerequisites
  - Enable push notifications in your app by [adding the Firebase dependency.](https://firebase.google.com/docs/cloud-messaging/android/client)
- Developer Documentation
  - [API usage](./sources/api-usage.md#push-messaging-apis)
  - [PushTrackingStatus](./sources/enum-public-classes/enum-push-tracking-status.md)
  - [MessagingPushPayload](./sources/enum-public-classes/messaging-push-payload.md)
- Guides
  - [Handling the received push payload](./sources/enum-public-classes/messaging-push-payload.md)
  - [Manual display and tracking of push notification](./sources/push-notification/manual-handling-and-tracking.md)
  - [Automatic display and tracking of push notification](./sources/push-notification/automatic-handling-and-tracking.md)

### In-app Messaging

- Developer Documentation
  - [API usage](./sources/api-usage.md#in-app-messaging-apis)
  - [Message](./sources/enum-public-classes/class-message.md)
- Guides
  - [Programmatically control the display of in-app messages](./sources/in-app-messaging/how-to-presentation-delegate.md)
  - [Call native code from the Javascript of an in-app message](./sources/in-app-messaging/how-to-call-native-from-javascript.md)
  - [Execute Javascript code in an in-app message from native code](./sources/in-app-messaging/how-to-call-javascript-from-native.md)
  - [Handle URL clicks from an in-app message](./sources/in-app-messaging/how-to-handle-url-clicks.md)

### Code-based experiences and content cards

- Developer Documentation
  - [API usage](./sources/api-usage.md#code-based-experiences-and-content-cards-apis)
- Public classes
  - [ContentCard - DEPRECATED](./sources/propositions/content-card.md)
  - [ContentType](./sources/propositions/schemas/content-type.md)
  - [Proposition](./sources/propositions/proposition.md)
  - [PropositionItem](./sources/propositions/proposition-item.md)
  - [Surface](./sources/propositions/surface.md)
- Schema classes
  - [ContentCardSchemaData](./sources/propositions/schemas/content-card-schema-data.md)
  - [HtmlContentSchemaData](./sources/propositions/schemas/html-content-schema-data.md)
  - [InAppSchemaData](./sources/propositions/schemas/inapp-schema-data.md)
  - [JsonContentSchemaData](./sources/propositions/schemas/json-content-schema-data.md)

### Content cards with UI

- Developer documentation 

    - [API Usage](./sources/content-card-ui/api-usage.md)
- Public Classes, Enums, and Interfaces

    - [AepUI](./sources/content-card-ui/public-classes/aepui.md)
    - [ContentCardMapper](./sources/content-card-ui/public-classes/contentcardmapper.md)
    - [ContentCardUIEventListener](./sources/content-card-ui/public-classes/contentcarduieventlistener.md)
    - [UIAction](./sources/content-card-ui/public-classes/uiaction.md)
    - [UIEvent](./sources/content-card-ui/public-classes/uievent.md)
- UI Models

    - [AepButton](./sources/content-card-ui/public-classes/UIModels/aepbutton.md)
    - [AepIcon](./sources/content-card-ui/public-classes/UIModels/aepicon.md)
    - [AepImage](./sources/content-card-ui/public-classes/UIModels/aepimage.md)
    - [AepText](./sources/content-card-ui/public-classes/UIModels/aeptext.md)
    - [AepUITemplate](./sources/content-card-ui/public-classes/UIModels/aepuitemplate.md)
    - [AepUITemplateType](./sources/content-card-ui/public-classes/UIModels/aepuitemplatetype.md)
    - [SmallImageTemplate](./sources/content-card-ui/public-classes/UIModels/smallimagetemplate.md)
- Content Providers
    - [AepUIContentProvider](./sources/content-card-ui/public-classes/ContentProvider/aepuicontentprovider.md)
    - [ContentCardUIProvider](./sources/content-card-ui/public-classes/ContentProvider/contentcarduiprovider.md)
- Observers
    - [AepUIEventObserver](./sources/content-card-ui/public-classes/Observers/aepuieventobserver.md)
    - [ContentCardEventObserver](./sources/content-card-ui/public-classes/Observers/contentcardeventobserver.md)
- State
    - [AepCardUIState](./sources/content-card-ui/public-classes/State/aepcarduistate.md)
    - [SmallImageCardUIState](./sources/content-card-ui/public-classes/State/smallimagecarduistate.md)
- Styles
    - [AepButtonStyle](./sources/content-card-ui/public-classes/Styles/aepbuttonstyle.md)
    - [AepCardStyle](./sources/content-card-ui/public-classes/Styles/aepcardstyle.md)
    - [AepColumnStyle](./sources/content-card-ui/public-classes/Styles/aepcolumnstyle.md)
    - [AepIconStyle](./sources/content-card-ui/public-classes/Styles/aepiconstyle.md)
    - [AepImageStyle](./sources/content-card-ui/public-classes/Styles/aepimagestyle.md)
    - [AepRowStyle](./sources/content-card-ui/public-classes/Styles/aeprowstyle.md)
    - [AepTextStyle](./sources/content-card-ui/public-classes/Styles/aeptextstyle.md)
    - [SmallImageUIStyle](./sources/content-card-ui/public-classes/Styles/smallimageuistyle.md)
- Tutorials

    - [Fetch and Display Content Cards](./sources/content-card-ui/tutorial/displaying-content-cards.md) 
    - [Customizing Content Card Templates](./sources/content-card-ui/tutorial/customizing-content-card-templates.md)
    - [Listening to Content Card Events](./sources/content-card-ui/tutorial/listening-content-card-events.md)

### Common public classes, methods, and enums

- [MessagingEdgeEventType](./sources/enum-public-classes/enum-messaging-edge-event-type.md)
