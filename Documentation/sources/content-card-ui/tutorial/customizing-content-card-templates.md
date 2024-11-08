# Customizing Content Card Templates style

This tutorial explains how to customize the UI of content cards in your application.

## Overview

The Messaging extension provides a way to customize content cards by using [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers?hl=en) in conjunction with additional parameters provided when creating a [style](../public-classes/Styles/README.md) object. Refer to each individual style class to see the customizable settings available for each style.

## Setting custom style parameters when creating a `SmallImageUIStyle`

Perform the following steps to customize content card templates:

1. Create a style object with a `Modifier` as well as any additional customizations passed in as part of the constructor parameters (e.g. if customizing an [AepButtonStyle](../public-classes/Styles/aepbuttonstyle.md) , then along with the `Modifier`, the enabled state, elevation, shape, border, color, and padding can be customized if desired.)
2. Use the created style object when invoking the builder of the template style object (e.g. `SmallImageUIStyle`)

Below is an example implementation:

```kotlin
// create a custom style for the small image card in row
val smallImageCardStyleRow = SmallImageUIStyle.Builder()
	.cardStyle(AepCardStyle(modifier = Modifier.width(400.dp).height(200.dp)))
  .rootRowStyle(AepRowStyle(modifier = Modifier.fillMaxSize()))
	.imageStyle(AepImageStyle(modifier = Modifier.width(100.dp).height(100.dp)))
	.buttonRowStyle(AepRowStyle(modifier = Modifier.fillMaxSize()))
	.buttonStyle(arrayOf(Pair(AepButtonStyle(modifier = Modifier.padding(8.dp)),
                            AepTextStyle(textStyle = TextStyle(color = Color.Green, fontSize = 16.sp)))))
	.dismissButtonStyle(AepIconStyle(modifier = Modifier.padding(8.dp)))
  .dismissButtonAlignment(Alignment.TopEnd)
	.textColumnStyle(AepColumnStyle(modifier = Modifier.fillMaxSize()))
	.bodyAepTextStyle(AepTextStyle(textStyle = TextStyle(Color.Yellow)))
	.titleAepTextStyle(AepTextStyle(textStyle = TextStyle(Color.Green)))
	.build()

// Create row with composables from AepUI instances
LazyRow {
  items(reorderedAepUIList) { aepUI ->                   
    when (aepUI) {
      is SmallImageUI -> {
        val state = aepUI.getState()
        if (!state.dismissed) 
        {
          SmallImageCard(ui = aepUI, 
                         style = smallImageCardStyleRow, // setting the custom style here
                         observer = ContentCardEventObserver(contentCardCallback))
        }
      }
    }
  }
}
```
