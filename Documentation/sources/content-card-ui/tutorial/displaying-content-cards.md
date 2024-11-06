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

To display content cards in your app, use the `AepUI` objects returned by the `getContentCardsUI` API. The `AepUI` objects provide the user interface for templated content cards in your application.

### Display Content Cards in Compose UI application

Below is an example of how to display content cards in a Compose UI application:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)

  binding = ActivityScrollingBinding.inflate(layoutInflater)
  setContentView(binding.root)

  val surfaces = mutableListOf<Surface>()
  val surface = Surface("homepage")
  surfaces.add(surface)

  // Initialize the ContentCardUIProvider
  contentCardUIProvider = ContentCardUIProvider(surface)

  // Initialize the ViewModel
  contentCardViewModel =
  ViewModelProvider(this, AepContentCardViewModelFactory(contentCardUIProvider)).get(
    AepContentCardViewModel::class.java
  )

  contentCardCallback = ContentCardCallback()

  // Set a click listener for refresh button which calls the API for fetch content cards from Edge
  val refreshButton: ImageButton = findViewById(R.id.refreshButton)
  refreshButton.setOnClickListener {
    Messaging.updatePropositionsForSurfaces(surfaces)
    contentCardViewModel.refreshContent()
  }

  binding.composeView.apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      AppTheme {
        AepContentCardList(contentCardViewModel)
      }
    }
  }
}

@Composable
private fun AepContentCardList(viewModel: AepContentCardViewModel) {
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

#### Optional

You may choose to order your Cards by the priority value entered in the AJO UI. To do this, sort the returned array:

```kotlin
// Reorder the AepUI list based on the ContentCardSchemaData fields if needed
val reorderedAepUIList = aepUiList.sortedWith(compareByDescending {
  val rank =
  contentCardSchemaDataList[aepUiList.indexOf(it)]?.meta?.get("priority") as String?
  ?: "0"
  rank.toInt()
})
```
