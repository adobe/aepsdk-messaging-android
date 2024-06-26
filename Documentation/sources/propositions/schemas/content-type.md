# ContentType

Enum representing content types found within a schema object.

```java
public enum ContentType {
  APPLICATION_JSON(0),
  TEXT_HTML(1),
  TEXT_XML(2),
  TEXT_PLAIN(3),
  UNKNOWN(4);
  
  private final int value;

  ContentType(final int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  @Override
  public String toString() {
    switch (this) {
      case APPLICATION_JSON:
        return MessagingConstants.ContentTypes.APPLICATION_JSON;
      case TEXT_HTML:
        return MessagingConstants.ContentTypes.TEXT_HTML;
      case TEXT_XML:
        return MessagingConstants.ContentTypes.TEXT_XML;
      case TEXT_PLAIN:
        return MessagingConstants.ContentTypes.TEXT_PLAIN;
      default:
        return "";
    }
  }

  static ContentType fromString(final String typeString) {
    if (typeString == null) {
      return UNKNOWN;
    }
    ContentType contentType;
    switch (typeString) {
      case MessagingConstants.ContentTypes.APPLICATION_JSON:
        contentType = ContentType.APPLICATION_JSON;
        break;
      case MessagingConstants.ContentTypes.TEXT_HTML:
        contentType = ContentType.TEXT_HTML;
        break;
      case MessagingConstants.ContentTypes.TEXT_XML:
        contentType = ContentType.TEXT_XML;
        break;
      case MessagingConstants.ContentTypes.TEXT_PLAIN:
        contentType = ContentType.TEXT_PLAIN;
        break;
      default:
        contentType = ContentType.UNKNOWN;
    }
    return contentType;
  }
}
```

#### String values

Below is the table of values returned by calling the `toString` method for each case:

| Case | String value |
| ---- | ------------ |
| `APPLICATION_JSON` | `application/json` |
| `TEXT_HTML` | `text/html` |
| `TEXT_XML` | `text/xml` |
| `TEXT_PLAIN` | `text/plain` |
| `UNKNOWN` | (empty string) |
