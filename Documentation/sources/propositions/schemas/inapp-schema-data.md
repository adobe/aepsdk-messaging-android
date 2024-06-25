# InAppSchemaData

Represents the schema data object for an in-app schema.

```java
public class InAppSchemaData implements SchemaData {
  // Represents the content of the InAppSchemaData object.  Its value's type is determined by `contentType`.
  private Object content = null;
    
  // Determines the value type of `content`.
  private ContentType contentType = null;
    
  // Date and time this in-app campaign was published represented as epoch seconds
  private int publishedDate = 0;
    
  // Date and time this in-app campaign will expire represented as epoch seconds
  private int expiryDate = 0;
    
  // Map containing any additional meta data for this content card
  private Map<String, Object> meta = null;
    
  // Map containing parameters that help control display and behavior of the in-app message on mobile
  private Map<String, Object> mobileParameters = null;
    
  // Map containing parameters that help control display and behavior of the in-app message on web
  private Map<String, Object> webParameters = null;
    
  // List of remote assets to be downloaded and cached for future use with the in-app message
  private List<String> remoteAssets = null;

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

  @Nullable public Map<String, Object> getMobileParameters() {
    return mobileParameters;
  }

  @Nullable public Map<String, Object> getWebParameters() {
    return webParameters;
  }

  @Nullable public List<String> getRemoteAssets() {
    return remoteAssets;
  }
}
```
