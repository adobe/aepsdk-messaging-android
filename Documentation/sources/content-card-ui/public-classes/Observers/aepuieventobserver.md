# Interface - AepUIEventObserver

Interface for observing events related to AEP UI components. This interface defines a mechanism for handling various types of events triggered by lifecycle changes or user interactions with UI elements, such as display, dismiss, or user interaction events.
## Interface Definition

```kotlin
interface AepUIEventObserver {
    fun onEvent(event: UIEvent<*, *>)
}
```

## Methods

### onEvent

Called when an event related to a UI template occurs.

#### Parameters

- _event_ - The event to handle. Implementers can provide specific logic based on the type of UIEvent

#### Syntax

```kotlin
fun onEvent(event: UIEvent<*, *>)
```
