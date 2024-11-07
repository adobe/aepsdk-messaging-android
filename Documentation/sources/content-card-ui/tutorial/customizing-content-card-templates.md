# Customizing Content Card Templates

This tutorial explains how to customize the UI of content cards in your application.

## Overview

The Messaging extension provides a way to customize content cards using [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers?hl=en). You can customize the content card templates by defining an overriden [style](../public-classes/Styles/README.md) that is then applied to the overall template style (e.g. [SmallImageUIStyle](../public-classes/Styles/smallimageuistyle.md)).

## Creating a custom style using the Compose Modifier 

Perform the following steps to customize content card templates:

1. Create a style object with the desired customizations within the `Modifier`
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
