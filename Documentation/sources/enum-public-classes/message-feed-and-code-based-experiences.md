# Message Feed and Code Based Experiences classes

### class Surface

Represents an entity for user or system interaction. It is identified by a self-describing URI and is used to fetch the decision propositions from the AJO campaigns. For example, all mobile application surface URIs start with `mobileapp://`, followed by application package name and an optional path. 

#### Java

##### Syntax

```java
/// `Surface` class is used to create surfaces for requesting propositions in personalization query requests.
public class Surface implements Serializable {
    // Unique surface URI string
    private String uri;

    /**
     * Creates a new surface by appending the given surface `path` to the mobile app surface prefix.
     *
     * @return {@link String} containing the surface path.
     */
    public Surface(final String path) {...}

    /**
     * Creates a new base surface by appending application package name to the mobile app surface prefix {@literal mobileapp://}.
     */
    public Surface() {...}

    /**
     * Returns this surface's URI as a {@code String}.
     */
    public String getUri() {
        return uri;
    }
    ...
}
```

##### Example

```java
// Creates a surface instance representing a banner view within homeActivity in my mobile application.
final Surface surface = new Surface("homeActivity#banner")
```

### class MessagingProposition

Represents the decision propositions received from the remote, upon a personalization query request to the Experience Edge network.

```java
public class MessagingProposition implements Serializable {
    /**
     * Constructor
     */
    public MessagingProposition(final String uniqueId, final String scope, final Map<String, Object> scopeDetails, final List<MessagingPropositionItem> messagingPropositionItems) {...}

    /**
     * Returns this proposition's unique identifier as a {@code String}.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Returns this proposition's scope as a {@code String}.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns this proposition's scope details as a {@code Map<String, Object>}.
     */
    public Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    /**
     * Returns this proposition's items as a {@code List<MessagingPropositionItem>}.
     */
    public List<MessagingPropositionItem> getItems() {
        return messagingPropositionItems;
    }
    ...
}
```

### class MessagingPropositionItem

Represents the decision proposition item received from the remote, upon a personalization query to the Experience Edge network.

```java
public class MessagingPropositionItem implements Serializable {
    /**
     * Constructor
     */
    public MessagingPropositionItem(final String uniqueId, final String schema, final String content) {...}
    
    /**
     * Returns this proposition item's unique identifier as a {@code String}.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Returns this proposition item's schema as a {@code String}.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Returns this proposition item's content as a {@code String}.
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns this proposition item's parent {@code MessagingProposition}.
     */
    public MessagingProposition getProposition() {
        return propositionReference.get();
    }

    /**
     * Returns {@code Inbound} object created from this proposition item's content.
     */
    public Inbound decodeContent() {...}
    ...
}
```