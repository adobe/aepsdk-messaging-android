# Class - ContentCardUIProvider

Messaging extension implementation of [AepUIContentProvider](./aepuicontentprovider.md). ContentCardUiProvider is responsible for fetching and managing the content for a given surface. It uses Adobe Messaging APIs to retrieve propositions and transform them into UI templates for display.

## Methods

### getContent

Retrieves a flow of AepUITemplate lists for the given surface. The flow emits updates whenever new content is fetched.

#### Returns

A flow that emits lists of  [AepUITemplate](../UIModels/aepuitemplate.md)s.

#### Syntax

```kotlin
override suspend fun getContent(): Flow<List<AepUITemplate>>
```

### getContentCardUI

Retrieves a flow of AepUI instances for the given surface. This function initiates the content fetch using [getContent](./contentcarduiprovider.md#getcontent) and then returns a flow of AepUI instances that represent the UI templates. The flow emits updates whenever new content is fetched or any changes occur.

#### Returns

A [Flow](https://developer.android.com/kotlin/flow) that emits a list of AepUI instances.

#### Syntax

```kotlin
suspend fun getContentCardUI(): Flow<List<AepUI<*, *>>>
```

### refreshContent

Updates the flow returned by [getContent](#getContent) with the latest cached content cards for the given surface.

#### Syntax

```kotlin
override suspend fun refreshContent()
```

