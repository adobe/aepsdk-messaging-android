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

## Offline Content Card Availability

By default, fetched content cards are cached in-memory only and are lost when the application is terminated, so a network connection is required to fetch them again on the next launch.

To make content cards available across launches — including when the device is offline — enable offline content card availability. This feature is **opt-in** and is **disabled by default**.

To enable it, use the Adobe Experience Platform Data Collection (Launch) configuration UI: while installing or configuring the Adobe Journey Optimizer extension in your mobile property, check the **Content Card Offline Available** checkbox. This sets the `messaging.contentCardOfflineAvailable` configuration key to `true`.

When enabled, qualified content cards are persisted to disk and are hydrated at app launch before the initial network fetch, so previously delivered cards are available immediately on a cold start, even without connectivity.

> Note - Content cards displayed from the persisted cache before a live network refresh are reported with `servedFromPersistentCache = true` in their display tracking. Once a network response refreshes the surface in the current session, subsequent displays are reported with `servedFromPersistentCache = false`.

To clear persisted (and in-memory) content cards — for example after a user logs out — use the [clearCachedPropositions](../../api-usage.md#clearcachedpropositions) API. Content cards are also cleared automatically on an identity reset.

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
