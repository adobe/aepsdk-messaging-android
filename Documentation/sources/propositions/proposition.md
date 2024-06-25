# Proposition

Represents the decision propositions received from the remote, upon a personalization query request to the Experience Edge network.

```java
public class Proposition implements Serializable {
  // Unique proposition identifier
  private final String uniqueId;
  // Scope string
  private final String scope;
  // Scope details map
  private final Map<String, Object> scopeDetails;
  // List containing proposition decision items
  private final List<PropositionItem> propositionItems = new ArrayList<>();

  ...
}
```