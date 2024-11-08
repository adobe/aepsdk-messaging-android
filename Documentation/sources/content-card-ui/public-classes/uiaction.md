# Sealed Class - UIAction

Represents an action that can be performed on a UI component.

#### Syntax

``` kotlin
sealed class UIAction
```

## Data Class - Click

Represents a click UIAction that can be performed on a UI component.

#### Public Properties

| Property  | Type    | Description                                                |
| --------- | ------- | ---------------------------------------------------------- |
| id        | String  | unique identifier of the UI component                      |
| actionUrl | String? | optional URL to be opened when the UI component is clicked |

#### Syntax

``` kotlin
data class Click(val id: String, val actionUrl: String?) : UIAction()
```

