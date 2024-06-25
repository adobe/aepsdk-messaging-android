# Surface

Represents an entity for user or system interaction. It is identified by a self-describing URI and is used to fetch decision propositions from AJO campaigns. 

All mobile application `Surface` URIs start with `mobileapp://`, followed by the app bundle identifier, and finally an optional path. 

#### Java

##### Syntax

```java
// An entity uniquely defined by a URI that can be interacted with.
public class Surface implements Serializable {

  // Unique surface URI string
  private String uri;
  
  public String getUri() {
    return uri;
  }
  
  ...
  
  // Creates a new surface by appending the given surface uri to the mobile app surface prefix.
  static Surface fromUriString(final String uri) {
    final Surface surface = new Surface(true, uri);
    return !surface.isValid() ? null : surface;
  }
}
```

##### Example

```java
// Creates a surface instance representing a banner within homeView view in my mobile application.
final Surface surface = Surface.fromUriString("homeView#banner");
```