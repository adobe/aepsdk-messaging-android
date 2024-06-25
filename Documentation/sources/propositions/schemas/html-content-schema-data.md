# HtmlContentSchemaData

Represents the schema data object for an HTML content schema.

```java
public class HtmlContentSchemaData implements SchemaData {
  // Represents the content of the HtmlContentSchemaData object.
  private String content = null;
    
  // Determines the value type of `content`.  For HtmlContentSchemaData objects, this value is always `ContentType.TEXT_HTML`.
  private ContentType format = null;

  ...
    
  @Override
  @Nullable public String getContent() {
    return content;
  }

  public ContentType getFormat() {
    return format;
  }
}
```
