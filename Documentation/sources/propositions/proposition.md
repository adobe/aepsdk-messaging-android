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
  
  /**
   * Gets the {@code Proposition} identifier.
   *
   * @return {@link String} containing the {@link Proposition} identifier.
   */
  @NonNull public String getUniqueId() {
      return uniqueId;
  }

  /**
   * Gets the {@code PropositionItem} list.
   *
   * @return {@code List<PropositionItem>} containing the {@link PropositionItem}s.
   */
  @NonNull public List<PropositionItem> getItems() {
      return propositionItems;
  }

  /**
   * Gets the {@code Proposition} scope.
   *
   * @return {@link String} containing the encoded {@link Proposition} scope.
   */
  @NonNull public String getScope() {
      return scope;
  }

  ...
}
```