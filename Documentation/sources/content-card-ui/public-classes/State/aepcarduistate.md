# Open Class - AepCardUIState

Class representing the state of an AEP card. This class includes the properties `dismissed` and `displayed` which are common across different card states.
## Class Definition

```kotlin
open class AepCardUIState(
    open val dismissed: Boolean,
    open val displayed: Boolean
)
```

## Public Properties

| Property  | Type    | Description                                    |
| --------- | ------- | ---------------------------------------------- |
| dismissed | Boolean | Indicates whether the card has been dismissed. |
| displayed | Boolean | Indicates whether the card has been displayed. |
