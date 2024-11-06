# Sealed Class - UIEvent

Represents different types of UI events that can be triggered by the user interaction on the UI templates.
## Methods

### getTemplate 

Retrieves the template associated with this UI component.

#### Syntax

``` kotlin
fun getTemplate(): T
```

### getState 

Retrieves the current state of the UI component.

#### Syntax

``` kotlin
fun getState(): S
```

### updateState 

Updates the state of the UI component with a new state.

#### Parameters

- _newState_ - The new state of type `S` to update within the UI component.

#### Syntax

``` kotlin
fun updateState(newState: S)
```

