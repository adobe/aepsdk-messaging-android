# PropositionItem

Represents the decision proposition item received from the remote, upon a personalization query to the Experience Edge network.

```java
public class PropositionItem implements Serializable {
  // Unique identifier for this `PropositionItem`
  // contains value for `id` in JSON
  private String itemId;

  // `PropositionItem` schema string
  // contains value for `schema` in JSON
  private SchemaType schema;

  // PropositionItem data map containing the JSON data
  private Map<String, Object> itemData;

  // Soft reference to Proposition instance
  SoftReference<Proposition> propositionReference;
  
  /**
   * Gets the {@code PropositionItem} identifier.
   *
   * @return {@link String} containing the {@link PropositionItem} identifier.
   */
  @NonNull public String getItemId() {
      return itemId;
  }

  /**
   * Gets the {@code PropositionItem} content schema.
   *
   * @return {@link SchemaType} containing the {@link PropositionItem} content schema.
   */
  @NonNull public SchemaType getSchema() {
      return schema;
  }

  /**
   * Gets the {@code PropositionItem} data.
   *
   * @return {@link Map<String, Object>} containing the {@link PropositionItem} data.
   */
  @NonNull public Map<String, Object> getItemData() {
      return itemData;
  }
  
  ...
}
```

# Public functions

## track

Tracks an interaction with the given `PropositionItem`.

```java
public void track(final String interaction, @NonNull final MessagingEdgeEventType eventType, final List<String> tokens)
```

#### Parameters

- _interaction_ - a custom `String` value to be recorded in the interaction
- _eventType_ - the [`MessagingEdgeEventType`](./../enum-public-classes/enum-messaging-edge-event-type.md) to be used for the ensuing Edge Event
- _tokens_ - a `List` containing the sub-item tokens for recording the interaction

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem

// tracking a display
propositionItem?.track("interaction", MessagingEdgeEventType.DISPLAY, null)

// tracking a user interaction
propositionItem?.track("userAccept", MessagingEdgeEventType.INTERACT, null)

// Extract the tokens from the PropositionItem's itemData map
propositionItem?.track("click", MessagingEdgeEventType.INTERACT, listOf("dataItemToken1", "dataItemToken2"))
```

#### Java

```java
PropositionItem propositionItem;

// tracking a display
propositionItem.track("interaction", MessagingEdgeEventType.DISPLAY, null);

// tracking a user interaction
propositionItem.track("userAccept", MessagingEdgeEventType.INTERACT,  null);

// Extract the tokens from the PropositionItem's itemData map
propositionItem.track("click", MessagingEdgeEventType.INTERACT, new ArrayList<String>() 
  {{
    add("dataItemToken1");
    add("dataItemToken2");
  }}
);
```

# Public calculated variables

## contentCardSchemaData

Tries to retrieve a `ContentCardSchemaData` object from this `PropositionItem`'s `content` property in `itemData`.

Returns a `ContentCardSchemaData` object if the schema for this `PropositionItem` is `SchemaType.CONTENT_CARD` and it is properly formed - `null` otherwise.

```java
@Nullable public ContentCardSchemaData getContentCardSchemaData()
```

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem
val contentCardSchemaData = propositionItem?.contentCardSchemaData
contentCardSchemaData?.let {
  // do something with the ContentCardSchemaData object
}
```

#### Java

```java
PropositionItem propositionItem;
ContentCardSchemaData contentCardSchemaData = propositionItem.getContentCardSchemaData();
if (contentCardSchemaData != null) {
    // do something with the ContentCardSchemaData object
}
```

## htmlContent

Tries to retrieve `content` from this `PropositionItem`'s `itemData` map as an HTML `String`.

Returns a string if the schema for this `PropositionItem` is `SchemaType.HTML_CONTENT` and it contains string content - `null` otherwise.

```java
@Nullable public String getHtmlContent()
```

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem
val htmlContent = propositionItem?.htmlContent
htmlContent?.let {
  // do something with the html content
}
```

#### Java

```java
PropositionItem propositionItem;
String htmlContent = propositionItem.htmlContent();
if (!StringUtils.isNullOrEmpty(htmlContent)) {
    // do something with the html content
}
```

## inAppSchemaData

Tries to retrieve an `InAppSchemaData` object from this `PropositionItem`'s `content` property in `itemData`.

Returns an `InAppSchemaData` object if the schema for this `PropositionItem` is `SchemaType.INAPP` and it is properly formed - `null` otherwise.

```java
@Nullable public InAppSchemaData getInAppSchemaData()
```

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem
val inAppSchemaData = propositionItem?.inAppSchemaData
inAppSchemaData?.let {
  // do something with the InAppSchemaData object
}
```

#### Java

```java
PropositionItem propositionItem;
InAppSchemaData inAppSchemaData = propositionItem.getInAppSchemaData();
if (inAppSchemaData != null) {
    // do something with the InAppSchemaData object
}
```

## jsonContentMap

Tries to retrieve `content` from this `PropositionItem`'s `itemData` map as an `Map<String, Object>`.

Returns a `Map` if the schema for this `PropositionItem` is `SchemaType.JSON_CONTENT` and it contains JSON content - `null` otherwise.

```java
@Nullable public Map<String, Object> getJsonContentMap() 
```

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem
val jsonContentMap = propositionItem?.jsonContentMap
jsonContentMap?.let {
  // do something with the map content
}
```

#### Java

```java
PropositionItem propositionItem;
Map<String, Object> jsonContentMap = propositionItem.getJsonContentMap();
if (jsonContentMap != null && !jsonContentMap.isEmpty()) {
    // do something with the map content
}
```

## jsonContentArrayList

Tries to retrieve `content` from this `PropositionItem`'s `itemData` map as a `Map<String, Object>` list.

Returns a `List` if the schema for this `PropositionItem` is `SchemaType.JSON_CONTENT` and it contains JSON content - `null` otherwise.

```java
@Nullable public List<Map<String, Object>> getJsonContentArrayList()
```

#### Example

#### Kotlin

```kotlin
val propositionItem: PropositionItem
val jsonContentList = propositionItem?.jsonContentArrayList
jsonContentList?.let {
  // do something with the list content
}
```

#### Java

```java
PropositionItem propositionItem;
List<Map<String, Object>> jsonContentList = propositionItem.getJsonContentArrayList();
if (jsonContentList != null && !jsonContentMap.isEmpty()) {
    // do something with the list content
}
```
