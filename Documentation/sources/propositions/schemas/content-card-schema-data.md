# ContentCardSchemaData

Represents the schema data object for a content-card schema.

```java
public class ContentCardSchemaData implements SchemaData {
    // Represents the content of the ContentCardSchemaData object.  Its value's type is determined by `contentType`.
    private Object content;
    
    // Determines the value type of `content`.
    private ContentType contentType;
    
    // Date and time this content card was published represented as epoch seconds
    private int publishedDate;
    
    // Date and time this content card will expire represented as epoch seconds
    private int expiryDate;
    
    // Dictionary containing any additional meta data for this content card
    private Map<String, Object> meta;

    ...
    @Override
    public Object getContent() {
      return content;
    }

    public ContentType getContentType() {
      return contentType;
    }

    public int getPublishedDate() {
      return publishedDate;
    }

    public int getExpiryDate() {
      return expiryDate;
    }

    @Nullable public Map<String, Object> getMeta() {
      return meta;
    }
}
```

# Public functions

## getContentCard

Tries to convert the `content` of this `ContentCardSchemaData` into a [`ContentCard`](./../content-card.md) object.

Returns `null` if the `contentType` is not equal to `ContentType.APPLICATION_JSON` or the data in `content` is not decodable into a `ContentCard`.

#### Syntax

```java
@Nullable public ContentCard getContentCard()
```

#### Example

```java
PropositionItem propositionItem;
ContentCardSchemaData contentCardSchemaData = propositionItem.getContentCardSchemaData();
ContentCard contentCard = contentCardSchemaData.getContentCard();
if (contentCard != null) {
  // do something with the ContentCard object
}
```

## track

Tracks an interaction with the given `ContentCardSchemaData`.

```java
public void track(final String interaction, final MessagingEdgeEventType eventType)
```

#### Parameters

- _interaction_ - a custom `String` value to be recorded in the interaction
- _eventType_ - the [`MessagingEdgeEventType`](./../../enum-public-classes/enum-messaging-edge-event-type.md) to be used for the ensuing Edge Event

#### Example

```java
ContentCardSchemaData contentCardSchemaData;

// tracking a display
contentCardSchemaData.track(null, MessagingEdgeEventType.DISPLAY);

// tracking a user interaction
contentCardSchemaData.track("itemSelected", MessagingEdgeEventType.INTERACT);
```
