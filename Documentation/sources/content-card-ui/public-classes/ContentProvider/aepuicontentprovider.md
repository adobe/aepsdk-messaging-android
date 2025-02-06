# Interface - AepUIContentProvider

Responsible for retrieving and refreshing data as required by the UI. 

Classes implementing this interface will define a strategy to provide content for rendering the UI.

## Interface Definition

```kotlin
interface AepUIContentProvider {
    suspend fun getContent(): Flow<List<AepUITemplate>>
    suspend fun refreshContent()
}
```

## Methods

### getContent

Retrieves the content for the UI.

#### Returns

The content for the UI as a flow of [AepUITemplate](../UIModels/aepuitemplate.md)s.

#### Syntax

```kotlin
suspend fun getContent(): Flow<List<AepUITemplate>>
```

### refreshContent

Refreshes the content for the UI. Implementations should update the data into the flow returned by [getContent](./aepuicontentprovider.md#getContent).

#### Syntax

```kotlin
suspend fun refreshContent()
```