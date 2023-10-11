# APIs Usage

This document details the Messaging SDK APIs that can be used to implement code-based experiences in mobile apps.

## Code-based experiences APIs

- [updatePropositionsForSurfaces](#updatePropositionsForSurfaces)
- [getPropositionsForSurfaces](#getPropositionsForSurfaces)

---

### updatePropositionsForSurfaces

Dispatches an event for the Edge network extension to fetch personalization decisions from the AJO campaigns for the provided surfaces array. The returned decision propositions are cached in-memory by the Messaging extension.

To retrieve previously cached decision propositions, use `getPropositionsForSurfaces` API.

#### Java

##### Syntax

```java
public static void updatePropositionsForSurfaces(@NonNull final List<Surface> surfaces)
```

##### Example

```java
final Surface surface1 = new Surface("myActivity#button");
final Surface surface2 = new Surface("myActivityAttributes");

final List<Surface> surfaces = new ArrayList<>();
surfaces.add(surface1);
surfaces.add(surface2);

Messaging.updatePropositionsForSurfaces(surfaces)
```

---

### getPropositionsForSurfaces

Retrieves the previously fetched propositions from the SDK's in-memory propositions cache for the provided surfaces. The callback is invoked with the decision propositions corresponding to the given surfaces or AdobeError, if it occurs. 

If a requested surface was not previously cached prior to calling `getPropositionsForSurfaces` (using the `updatePropositionsForSurfaces` API), no propositions will be returned for that surface.

#### Java

##### Syntax

```java
public static void getPropositionsForSurfaces(@NonNull final List<Surface> surfaces, @NonNull final AdobeCallback<Map<Surface, List<MessagingProposition>>> callback)
```

##### Example

```java
final Surface surface1 = new Surface("myActivity#button");
final Surface surface2 = new Surface("myActivityAttributes");

final List<Surface> surfaces = new ArrayList<>();
surfaces.add(surface1);
surfaces.add(surface2);

Messaging.getPropositionsForSurfaces(surfaces, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
    @Override
    public void fail(final AdobeError adobeError) {
        // handle error
    }

    @Override
    public void call(Map<Surface, List<MessagingProposition>> propositionsMap) {
        if (propositionsMap != null && !propositionsMap.isEmpty()) {
            // get the propositions for the given surfaces
            if (propositionsMap.contains(surface1)) {
                final List<MessagingProposition> propositions1 = propositionsMap.get(surface1)
                // read surface1 propositions
            }
            if (propositionsMap.contains(surface2)) {
                final List<MessagingProposition> proposition2 = propositionsMap.get(surface2)
                // read surface2 propositions
            }
        }
    }
});
```

---

## Public Classes

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