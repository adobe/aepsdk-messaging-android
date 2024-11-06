# Sealed Interface - AepUI

The `AepUI` interface represents a UI component that can be rendered using the AEP compose UI library. In the initial launch of the AEP compose UI library, the `SmallImageUI` implements an `AepUI`  to display a Small Image Template. The AepUI associates with a specific template and state to ensure consistency when rendering and managing UI components.
## Methods

### getTemplate 

Retrieves the template associated with this UI component.

#### Returns

A template of type `T` associated with the UI componenet.

#### Syntax

``` kotlin
fun getTemplate(): T
```

### getState 

Retrieves the current state of the UI component.

#### Returns

A state of type `S` associated with the UI componenet.

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

