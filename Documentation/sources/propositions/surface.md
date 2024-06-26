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
  
  public Surface(final String path) {}
  
  public Surface() {}
  
  ...
}
```

#### Example

#### Kotlin

```kotlin
// Creates a surface instance representing a banner within homeView view in my mobile application.
val surface = Surface("homeView#banner")
```

#### Java

```java
// Creates a surface instance representing a banner within homeView view in my mobile application.
final Surface surface = new Surface("homeView#banner");
```