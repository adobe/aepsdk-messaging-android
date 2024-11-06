# Class - SmallImageUI

Class representing a small image AEP UI, which implements the [AepUI](./aepui.md) interface.
## Methods

### getTemplate 

Retrieves the template associated with the small image UI.

#### Returns

The small image template.

#### Syntax

``` kotlin
override fun getTemplate(): SmallImageTemplate
```

### getState 

Retrieves the current state of the small image UI.

#### Returns

The current SmallImageCardUIState.

#### Syntax

``` kotlin
override fun getState(): SmallImageCardUIState
```

### updateState 

Updates the current state of the small image UI.

#### Parameters

- _newState_ - The new state of type `SmallImageCardUIState` to update within the UI component.

#### Syntax

``` kotlin
override fun updateState(newState: SmallImageCardUIState)
```

