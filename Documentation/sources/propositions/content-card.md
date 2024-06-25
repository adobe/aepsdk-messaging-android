# ContentCard

An object representing the default content card created in the Adobe Journey Optimizer UI. 

Content cards must be rendered by the app developer.  Tracking a content card is done via calls to the [`track`](#track) API.

```java
public class ContentCard {
  // Plain-text title for the content card
  private String title;
  // Plain-text body representing the content for the content card
  private String body;
  // String representing a URI that contains an image to be used for this content card
  private String imageUrl;
  // Contains a URL to be opened if the user interacts with the content card
  private String actionUrl;
  // Required if actionUrl is provided. Text to be used in title of button or link in content card
  private String actionTitle;
  // Reference to parent ContentCardSchemaData instance
  SoftReference<ContentCardSchemaData> parent;

  ...
}
```

# Public functions

## track

Tracks an interaction with the given `ContentCard`.

```java
public void track(final String interaction, final MessagingEdgeEventType eventType)
```

#### Parameters

- _interaction_ - a custom `String` value to be recorded in the interaction
- _eventType_ - the [`MessagingEdgeEventType`](./../enum-public-classes/enum-messaging-edge-event-type.md) to be used for the ensuing Edge Event

#### Example

```java
// Get content card schema data from a PropositionItem object
ContentCardSchemaData contentCardData = item.getContentCardSchemaData();
ContentCard contentCard = contentCardData.getContentCard();

// tracking a display
contentCard.track(MessagingEdgeEventType.DISPLAY)

// tracking a user interaction
contentCard.track("itemSelected", MessagingEdgeEventType.INTERACT)
```
