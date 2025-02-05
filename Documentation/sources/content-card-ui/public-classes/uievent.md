# Sealed Class - UIEvent

Represents different types of UI events that can be triggered by the user interaction on the UI templates.

#### Class Parameters

| Parameter | Type                                         | Description                                                  |
| --------- | -------------------------------------------- | ------------------------------------------------------------ |
| T         | [AepUITemplate](./UIModels/aepuitemplate.md) | Represents a UI template model which backs the composable on which the event has occurred. |
| S         | [AepCardUIState](./State/aepcarduistate.md)  | Represents the state of the AEP card composable on which the event has occurred. |

#### Syntax

``` kotlin
sealed class UIEvent<T : AepUITemplate, S : AepCardUIState>(open val aepUi: AepUI<T, S>)
```

#### Public Properties

| Property | Type                | Description                          |
| -------- | ------------------- | ------------------------------------ |
| aepUI    | [AepUI](./aepui.md) | The AepUI associated with the event. |

## Data Class - Display

Event that represents the display of a UI element.

#### Syntax

``` kotlin
data class Display<T : AepUITemplate, S : AepCardUIState>(override val aepUi: AepUI<T, S>) :
        UIEvent<T, S>(aepUi)
```

## Data Class - Interact

Event that represents a user interaction with a UI element. The `Interact` event captures the different types of interactions that a user can have with a UI component. Currently supported interactions types are can be seen in [UIAction](./uiaction.md) documentation.

#### Public Properties

| Property | Type                      | Description                 |
| -------- | ------------------------- | --------------------------- |
| action   | [UIAction](./uiaction.md) | The UIAction that occurred. |

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

#### Syntax

``` kotlin
data class Dismiss<T : AepUITemplate, S : AepCardUIState>(override val aepUi: AepUI<T, S>) :
        UIEvent<T, S>(aepUi)
```
