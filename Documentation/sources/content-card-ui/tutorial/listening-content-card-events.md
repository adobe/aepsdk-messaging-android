# Listening to Content Card Events

This tutorial explains how to listen to content card events in your application.

## Overview

The Messaging extension provides a way to listen to events from content cards displayed in your application. The following functions can be implemented in conformance with the `ContentCardUIEventListener` interface:

- `onDisplay`
- `onDismiss`
- `onInteract`

## Implement ContentCardUIEventListener

Complete the following steps to hear content card events:

1. Implement the [ContentCardUIEventListener](../public-classes/contentcarduieventlistener.md) interface in your class.

```kotlin
class ContentCardCallback: ContentCardUIEventListener {
  override fun onDisplay(aepUI: AepUI<*, *>) {
    Log.d("ContentCardCallback", "onDisplay")
  }

  override fun onDismiss(aepUI: AepUI<*, *>) {
    Log.d("ContentCardCallback", "onDismiss")
  }

  override fun onInteract(
    aepUI: AepUI<*, *>,
    interactionId: String?,
    actionUrl: String?
  ): Boolean {
    Log.d("ContentCardCallback", "onInteract $interactionId $actionUrl")
    // If the url is handled here, return true
    return false
  }
}
```

2. Pass the listener to the [ContentCardEventObservers](../public-classes/Observers/contentcardeventobserver.md) class when retrieving the card composable.

```kotlin
@Composable
private fun AepContentCardList(viewModel: AepContentCardViewModel) {
  // Create the ContentCardUIEventListener
  val contentCardCallback = ContentCardCallback()
  // Collect the state from ViewModel
  val aepUiList by viewModel.aepUIList.collectAsStateWithLifecycle()
  
  // Create row with composables from AepUI instances
  LazyRow {
    items(aepUiList) { aepUI ->
                               when (aepUI) {
                                 is SmallImageUI -> {
                                   val state = aepUI.getState()
                                   if (!state.dismissed) {
                                     SmallImageCard(
                                       ui = aepUI,
                                       style = smallImageCardStyleRow,
                                       // provide the ContentCardUIEventListener as a parameter to the 				  																	 // ContentCardEventObserver
                                       observer = ContentCardEventObserver(contentCardCallback)
                                     )
                                   }
                                 }
                               }
                              }
  }
}    
```

### Handling actionable URLs

The `onInteract` method provides an optional `actionURL` parameter associated with the interaction event. The return value of this method determines how the URL is handled.

- Return `true` if your application has successfully handled the URL. This indicates to the SDK that no further action is needed.

- Return `false` to allow the SDK to process the URL.

```kotlin
override fun onInteract(
  aepUI: AepUI<*, *>,
  interactionId: String?,
  actionUrl: String?
): Boolean {
  actionUrl?.let { 
    // handle action url here
    return true
  }
  return false
}
```
