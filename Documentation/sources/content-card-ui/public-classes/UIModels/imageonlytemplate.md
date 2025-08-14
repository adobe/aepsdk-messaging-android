# Data Class - ImageOnlyTemplate

Class representing an image-only template, which implements the [AepUITemplate](aepuitemplate.md) interface.

## Public Properties

| Property   | Type                       | Description                                                |
| ---------- | -------------------------- | ---------------------------------------------------------- |
| id         | String                     | The unique identifier for this template.                   |
| image      | [AepImage](./aepimage.md)  | The details of the image to be displayed.                  |
| actionUrl  | String?                    | The URL to be opened when the image-only card is clicked.  |
| dismissBtn | [AepIcon](./aepicon.md)?   | The details for the image-only template dismiss button.    |

## Methods

### getType 

Returns the type of this template, which is [AepUITemplateType](aepuitemplatetype.md).IMAGE_ONLY.

#### Syntax

``` kotlin
override fun getType() = AepUITemplateType.IMAGE_ONLY
```


