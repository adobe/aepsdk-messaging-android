# Class - LargeImageUIStyle

Class representing the style for a large image AEP UI.

![Large Image Card Composeable Layout](../../../../assets/large-image-card-layout.png)

## Public Properties

| Property               | Type                                                         | Description                                                  |
| ---------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| cardStyle              | [AepCardStyle](./aepcardstyle.md)                            | The style for the card.                                      |
| rootColumnStyle        | [AepColumnStyle](./aepcolumnstyle.md)                        | The style for the root column.                               |
| imageStyle             | [AepImageStyle](./aepimagestyle.md)                          | The style for the image.                                     |
| textColumnStyle        | [AepColumnStyle](./aepcolumnstyle.md)                        | The style for the column containing the title, body and buttons. |
| titleTextStyle         | [AepTextStyle](./aeptextstyle.md)                            | The text style for the title.                                |
| bodyTextStyle          | [AepTextStyle](./aeptextstyle.md)                            | The text style for the body.                                 |
| buttonRowStyle         | [AepRowStyle](./aeprowstyle.md)                              | The style for the row containing the buttons.                |
| buttonStyle            | Array<[AepButtonStyle](./aepbuttonstyle.md)>                 | The style for the buttons.                                   |
| dismissButtonStyle     | [AepIconStyle](./aepiconstyle.md)                            | The style for the dismiss button.                            |
| dismissButtonAlignment | [Alignment](https://developer.android.com/reference/kotlin/androidx/compose/ui/Alignment) | The alignment for the dismiss button.                        |

## Customization

The `LargeImageUIStyle` is created using a builder. Here's an example:

```kotlin
val largeImageStyle = LargeImageUIStyle.Builder()
    .cardStyle(AepCardStyle(modifier = Modifier.width(420.dp)))
    .imageStyle(AepImageStyle(contentScale = ContentScale.Fit))
    .titleAepTextStyle(AepTextStyle())
    .bodyAepTextStyle(AepTextStyle())
    .buttonRowStyle(AepRowStyle())
    .build()
```
