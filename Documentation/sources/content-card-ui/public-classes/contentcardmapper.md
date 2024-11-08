# Class - ContentCardMapper

Singleton class used to store a mapping between valid [ContentCardSchemaData](../../propositions/schemas/content-card-schema-data.md) and unique proposition id's. The schema data is used when sending proposition track requests to AJO.

## Class Definition

```kotlin
class ContentCardMapper private constructor() {
    private val contentCardSchemaDataMap: MutableMap<String, ContentCardSchemaData> = HashMap()

    companion object {
        @JvmStatic
        val instance: ContentCardMapper by lazy { ContentCardMapper() }
    }
}
```

## Methods

### getContentCardSchemaData

Returns a `ContentCardSchemaData` object for the given proposition id.

#### Parameters

- _propositionId_ - the proposition id to use as a key in the `ContentCardSchemaData` map.

#### Returns

The `ContentCardSchemaData` for the given proposition id, or null if not found

#### Syntax

```kotlin
fun getContentCardSchemaData(propositionId: String): ContentCardSchemaData?
```
