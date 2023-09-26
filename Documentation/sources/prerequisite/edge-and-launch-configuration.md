#  Configuring Edge & Launch

### Configure the Datastream
- To create the datastream follow the [datastream document](https://developer.adobe.com/client-sdks/documentation/getting-started/configure-datastreams/).
- When creating the datastream service for Adobe Experience Platform, ensure that the following are selected:
    * For Event Dataset, select any Dataset that is configured to hold the data from your application.
    * For Profile Dataset, select *AJO Push Profile Dataset*.
    * Ensure the *Edge Segmentation* box is checked.
    * Ensure the *Adobe Journey Optimizer* box is checked.

![Datastream](./../../assets/edge-config.png)

### Setup launch mobile property
- To create the mobile property follow [this document](https://developer.adobe.com/client-sdks/documentation/getting-started/create-a-mobile-property/)
- To configure AEP Edge Extension follow [this document](https://developer.adobe.com/client-sdks/documentation/edge-network/)
- To configure AEP Messaging Extension follow [this document](https://developer.adobe.com/client-sdks/documentation/adobe-journey-optimizer/)

### Now that a Mobile Property is created, head over to the [instructions](../setup-sdk.md) to install the SDK.
