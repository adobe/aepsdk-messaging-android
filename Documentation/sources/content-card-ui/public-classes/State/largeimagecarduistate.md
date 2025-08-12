# Data Class - LargeImageCardUIState

Class representing the UI state of a Large Image template card.

## Class Definition

```kotlin
data class LargeImageCardUIState(
    override val dismissed: Boolean = false,
    override val displayed: Boolean = false
) : AepCardUIState()
```

## Public Properties

| Property  | Type    | Description                                    |
| --------- | ------- | ---------------------------------------------- |
| dismissed | Boolean | Indicates whether the card has been dismissed. |
| displayed | Boolean | Indicates whether the card has been displayed. |
