# Data Class - SmallImageTemplate

Class representing a small image template, which implements the [AepUITemplate](aepuitemplate.md) interface.

## Public Properties

| Property   | Type                               | Description                                                  |
| ---------- | ---------------------------------- | ------------------------------------------------------------ |
| id         | String                             | The unique identifier for this template.                     |
| title      | [AepText](./aeptext.md)            | The title text and display settings.                         |
| body       | [AepText](./aeptext.md)?           | The body text and display settings.                          |
| image      | [AepImage](./aepimage.md)?         | The details of the image to be displayed.                    |
| actionUrl  | String?                            | If provided, interacting with this card will result in the opening of the actionUrl. |
| buttons    | List<[AepButton](./aepbutton.md)>? | The details for the small image template buttons.            |
| dismissBtn | [AepIcon](./aepicon.md)?           | The details for the small image template dismiss button.     |

## Methods

### getType 

Returns the type of this template, which is [AepUITemplateType](aepuitemplatetype.md).SMALL_IMAGE.

#### Syntax

``` kotlin
override fun getType() = AepUITemplateType.SMALL_IMAGE
```