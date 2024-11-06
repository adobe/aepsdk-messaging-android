# Sealed Class - UIAction

Represents an action that can be performed on a UI component.

#### Class Parameters

| Parameter | Type                                         | Description                                                  |
| --------- | -------------------------------------------- | ------------------------------------------------------------ |
| T         | [AepUITemplate](./UIModels/aepuitemplate.md) | Represents a UI template model associated with the acction like SmallImageTemplate, which backs the composable on which the event has occurred. |
| S         | [AepCardUIState](./State/aepcarduistate.md)  | Representing the state of the AEP card composable on which the event has occurred. |

#### Public Properties

| Property | Type                | Description                          |
| -------- | ------------------- | ------------------------------------ |
| aepUI    | [AepUI](./aepui.md) | The AepUI associated with the event. |

#### Syntax

``` kotlin
sealed class UIEvent<T : AepUITemplate, S : AepCardUIState>(open val aepUi: AepUI<T, S>)
```

## Data Class - Display

Event that represents the display of a UI element.

#### Class Parameters

| Parameter | Type                                         | Description                                                  |
| --------- | -------------------------------------------- | ------------------------------------------------------------ |
| T         | [AepUITemplate](./UIModels/aepuitemplate.md) | Represents a UI template model associated with the acction like SmallImageTemplate, which backs the composable on which the event has occurred. |
| S         | [AepCardUIState](./State/aepcarduistate.md)  | Representing the state of the AEP card composable on which the event has occurred. |

#### Public Properties

| Property | Type                | Description                                  |
| -------- | ------------------- | -------------------------------------------- |
| aepUI    | [AepUI](./aepui.md) | The AepUI associated with the display event. |

#### Syntax

``` kotlin
data class Display<T : AepUITemplate, S : AepCardUIState>(override val aepUi: AepUI<T, S>) :
        UIEvent<T, S>(aepUi)
```

## Data Class - Interact

Event that represents a user interaction with a UI element. The `Interact` event captures the different types of interactions that a user can have with a UI component, like clicking a button or expanding a card. Limiting the interaction types ensures consistency in event generation and handling.

#### Class Parameters

| Parameter | Type                                         | Description                                                  |
| --------- | -------------------------------------------- | ------------------------------------------------------------ |
| T         | [AepUITemplate](./UIModels/aepuitemplate.md) | Represents a UI template model associated with the acction like SmallImageTemplate, which backs the composable on which the event has occurred. |
| S         | [AepCardUIState](./State/aepcarduistate.md)  | Representing the state of the AEP card composable on which the event has occurred. |

#### Public Properties

| Property | Type                      | Description                                                  |
| -------- | ------------------------- | ------------------------------------------------------------ |
| aepUI    | [AepUI](./aepui.md)       | The AepUI associated with the interaction event, providing context about the UI component on which the interaction occurred. |
| action   | [UIAction](./uiaction.md) | The UIAction that occurred.                                  |

#### Syntax

``` kotlin
data class Interact<T : AepUITemplate, S : AepCardUIState>(
        override val aepUi: AepUI<T, S>,
        val action: UIAction
    ) : UIEvent<T, S>(aepUi)
```

#### Example

```kotlin
observer?.onEvent(AepUiEvent.Interact(ui, UIAction.Click(id = "purchaseID", actionUrl = "https://www.adobe.com"))
```

## Data Class - Dismiss

Event that represents the dismissal of a UI element.

#### Class Parameters

| Parameter | Type                                         | Description                                                  |
| --------- | -------------------------------------------- | ------------------------------------------------------------ |
| T         | [AepUITemplate](./UIModels/aepuitemplate.md) | Represents a UI template model associated with the acction like SmallImageTemplate, which backs the composable on which the event has occurred. |
| S         | [AepCardUIState](./State/aepcarduistate.md)  | Representing the state of the AEP card composable on which the event has occurred. |

#### Public Properties

| Property | Type                | Description                                  |
| -------- | ------------------- | -------------------------------------------- |
| aepUI    | [AepUI](./aepui.md) | The AepUI associated with the dismiss event. |

#### Syntax

``` kotlin
data class Dismiss<T : AepUITemplate, S : AepCardUIState>(override val aepUi: AepUI<T, S>) :
        UIEvent<T, S>(aepUi)
```
