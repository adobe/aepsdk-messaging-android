# Data Class - ImageOnlyCardUIState

Class representing the UI state of an Image Only template card.

## Class Definition

```kotlin
data class ImageOnlyCardUIState(
    override val dismissed: Boolean = false,
    override val displayed: Boolean = false
) : AepCardUIState()
```

## Public Properties

| Property  | Type    | Description                                    |
| --------- | ------- | ---------------------------------------------- |
| dismissed | Boolean | Indicates whether the card has been dismissed. |
| displayed | Boolean | Indicates whether the card has been displayed. |
