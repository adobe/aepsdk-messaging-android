# JsonContentSchemaData

Represents the schema data object for a json content schema.

```java
public class JsonContentSchemaData implements SchemaData {
  // Represents the content of the JsonContentSchemaData object.  Its value's type is determined by `format`.
  private Object content = null;
    
  // Determines the value type of `content`.
  private ContentType format = null;

  ...
  @Nullable Map<String, Object> getJsonObjectContent() {
    return content instanceof Map ? (Map<String, Object>) content : null;
  }

  @Nullable List<Map<String, Object>> getJsonArrayContent() {
    return content instanceof List ? (List<Map<String, Object>>) content : null;
  }
}
```