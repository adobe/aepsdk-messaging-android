# Data Class - SmallImageCardUIState

Class representing the UI state of a Small Image template card.
## Class Definition

```kotlin
data class SmallImageCardUIState(
    override val dismissed: Boolean = false,
    override val displayed: Boolean = false
) : AepCardUIState()
```

## Public Properties

| Property  | Type    | Description                                    |
| --------- | ------- | ---------------------------------------------- |
| dismissed | Boolean | Indicates whether the card has been dismissed. |
| displayed | Boolean | Indicates whether the card has been displayed. |
