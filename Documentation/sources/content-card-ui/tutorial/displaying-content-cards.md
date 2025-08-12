# Fetch and Display Content Cards

This tutorial explains how to fetch and display content cards in your application.

## Pre-requisites

[Integrate and register AEPMessaging extension](https://developer.adobe.com/client-sdks/edge/adobe-journey-optimizer/#implement-extension-in-mobile-app) in your app.

## Fetch Content Cards

To fetch the content cards for the surfaces configured in [Adobe Journey Optimizer](https://business.adobe.com/products/journey-optimizer/adobe-journey-optimizer.html) campaigns, call the [updatePropositionsForSurfaces](https://developer.adobe.com/client-sdks/edge/adobe-journey-optimizer/code-based/api-reference/#updatepropositionsforsurfaces) API. It's recommended to batch requests for multiple surfaces in a single API call when possible. The returned content cards are cached in-memory by the Messaging extension and persist through the application's lifecycle.

```kotlin
val surfaces = mutableListOf<Surface>()
val surface = Surface("homepage")
Messaging.updatePropositionsForSurfaces(surfaces)
```

## Retrieve Content Cards

To retrieve the content cards for a specific surface, call `getContentCardsUI`. This API returns a [flow](https://developer.android.com/kotlin/flow) of [AepUI](../public-classes/aepui.md) objects representing content cards for which the user is qualified.

`AepUI` objects are created only for content cards with templates recognized by the Messaging extension. The flow of `AepUI` objects may contain multiple content card template types.

```kotlin
// create a view model or reuse existing one to hold the aepUIList
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

    // Function to refresh the aepUIList from the ContentCardUIProvider
    fun refreshContent() {
        viewModelScope.launch {
            contentCardUIProvider.refreshContent()
        }
    }
}
```

> Note - only content cards for which the user has qualified are returned by the getContentCardUI API. Client-side rules are defined in the Adobe Journey Optimizer campaign.

## Display Content Cards

The Content Card user interface is implemented using Jetpack Compose, which is the recommended toolkit for Android development. To display content cards in your app, pass the `AepUI` objects returned by the `getContentCardUI` API to the appropriate Content Card composable. The currently supported composables are:
1. SmallImageCard composable for SmallImageUI

### Display Content Cards in Compose UI application

Below is an example of how to display content cards in a Compose UI application:

```kotlin
@Composable
private fun AepContentCardList(viewModel: AepContentCardViewModel) {
  // Collect the state from ViewModel
  val aepUiList by viewModel.aepUIList.collectAsStateWithLifecycle()
  
  // Create row with composables from AepUI instances
  LazyRow {
    items(reorderedAepUIList) { aepUI ->                   
      when (aepUI) {
        is SmallImageUI -> {
          val state = aepUI.getState()
          if (!state.dismissed) {
            SmallImageCard(
                ui = aepUI,
                style = SmallImageUIStyle.Builder().build(),
                observer = ContentCardEventObserver(contentCardCallback)
            )
          }
        }
        is LargeImageUI -> {
          val state = aepUI.getState()
          if (!state.dismissed) {
            LargeImageCard(
                ui = aepUI,
                style = LargeImageUIStyle.Builder().build(),
                observer = ContentCardEventObserver(contentCardCallback)
            )
          }
        }
        is ImageOnlyUI -> {
          val state = aepUI.getState()
          if (!state.dismissed) {
            ImageOnlyCard(
                ui = aepUI,
                style = ImageOnlyUIStyle.Builder().build(),
                observer = ContentCardEventObserver(contentCardCallback)
            )
          }
        }
      }
    }
  }
}    
```

Refer to this [TestApp](../../../../code/testapp/) for a complete example of how to display, customize and listen to UI events from content cards in a Compose UI application.

#### Retrieve ContentCardSchemaData from the Messaging extension

You may retrieve the `ContentCardSchemaData` for a Content Card using the template id using the [ContentCardMapper](../public-classes/contentcardmapper.md):

```kotlin
private fun AepContentCardList(viewModel: AepContentCardViewModel) {
  // Collect the state from ViewModel
  val aepUiList by viewModel.aepUIList.collectAsStateWithLifecycle()
  
  // Get the ContentCardSchemaData for the AepUI list if needed
  val contentCardSchemaDataList = aepUiList.map {
    when (it) {
      is SmallImageUI ->
      	ContentCardMapper.Companion.instance.getContentCardSchemaData(it.getTemplate().id)
      
      	else -> null
    }
  }
```
