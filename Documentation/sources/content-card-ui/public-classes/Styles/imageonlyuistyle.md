# Class - ImageOnlyUIStyle

Class representing the style for an image-only AEP UI.

## Public Properties

| Property               | Type                                                         | Description                                                  |
| ---------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| cardStyle              | [AepCardStyle](./aepcardstyle.md)                            | The style for the card.                                      |
| imageStyle             | [AepImageStyle](./aepimagestyle.md)                          | The style for the image.                                     |
| dismissButtonStyle     | [AepIconStyle](./aepiconstyle.md)                            | The style for the dismiss button.                            |
| dismissButtonAlignment | [Alignment](https://developer.android.com/reference/kotlin/androidx/compose/ui/Alignment) | The alignment for the dismiss button.                        |

## Customization

The `ImageOnlyUIStyle` is created using a builder. Here's an example:

```kotlin
val imageOnlyStyle = ImageOnlyUIStyle.Builder()
    .cardStyle(AepCardStyle(modifier = Modifier.width(320.dp)))
    .imageStyle(AepImageStyle(contentScale = ContentScale.Fit))
    .build()
```
