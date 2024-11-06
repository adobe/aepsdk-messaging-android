# APIs Usage

This document provides information on how to use the Messaging APIs to receive and display content card views in your application.

## Importing Messaging

To use the Messaging APIs, you need to import the Messaging extension in your Kotlin file.

```kotlin
import com.adobe.marketing.mobile.Messaging
```

## APIs

### getContentCardUI

The `getContentCardsUI` method retrieves a flow of [AepUI](./public-classes/aepui.md) objects for the provided surface. These AepUI objects provide the user interface for templated content cards in your application.

> **Note** - Calling this API will not download content cards from Adobe Journey Optimizer; it will only retrieve the content cards that are already downloaded and cached by the Messaging extension. You **must** call [`updatePropositionsForSurfaces`](../api-usage.md#updatePropositionsForSurfaces) API from the AEPMessaging extension with the desired surfaces prior to calling this API. 

#### Syntax

```kotlin
suspend fun getContentCardUI(): Flow<List<AepUI<*, *>>>
```

#### Example

```kotlin
// create new view model or reuse existing one to hold the aepUIList
class AepContentCardViewModel(private val contentCardUIProvider: ContentCardUIProvider) : ViewModel() {
    // State to hold AepUI list
    private val _aepUIList = MutableStateFlow<List<AepUI<*, *>>>(emptyList())
    val aepUIList: StateFlow<List<AepUI<*, *>>> = _aepUIList.asStateFlow()

    init {
        // Launch a coroutine to fetch the aepUIList from the ContentCardUIProvider
        // when the ViewModel is created
        viewModelScope.launch {
            contentCardUIProvider.getContentCardUI().collect { aepUi ->
                _aepUIList.value = aepUi
            }
        }
    }
}
```