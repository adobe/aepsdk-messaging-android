/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepuitemplates.utils

/**
 * Object containing constant values used throughout the application.
 *
 * This class is structured to hold constants related to card templates, including schema data,
 * UI elements, and specific styling details. These constants are referenced in various UI model
 * classes to ensure consistency and avoid hardcoding string values.
 */
internal class AepUIConstants {

    class CardTemplate {

        class SchemaData {
            companion object {
                const val TITLE = "title" // Key for the title in the schema
                const val BODY = "body" // Key for the body text in the schema
                const val IMAGE = "image" // Key for the image in the schema
                const val ACTION_URL = "actionUrl" // Key for the action URL in the schema
                const val BUTTONS = "buttons" // Key for the buttons in the schema
                const val DISMISS_BTN = "dismissBtn" // Key for the dismiss button in the schema
            }

            class Meta {
                companion object {
                    const val ADOBE_DATA = "adobe" // Key for Adobe-specific data
                    const val TEMPLATE = "template" // Key for template information
                }
            }
        }

        class UIElement {
            class Text {
                companion object {
                    const val CONTENT = "content" // Key for the content of text elements
                    const val CLR = "clr" // Key for the color of text elements
                    const val ALIGN = "align" // Key for the alignment of text elements
                    const val FONT = "font" // Key for the font of text elements
                }
            }

            class DismissButton {
                companion object {
                    const val STYLE = "style" // Key for the style of the dismiss button
                    const val NONE_ICON = "none" // no icon for the dismiss button, default value
                    const val SIMPLE_ICON = "icon" // icon style for the of the dismiss button
                    const val CIRCLE_ICON = "circle" // circle icon style for the of the dismiss button
                }

                class Icon {
                    companion object {
                        const val SIMPLE = "xmark" // Key for a simple icon
                        const val CIRCLE = "xmark.circle.fill" // Key for a filled circle icon
                    }
                }
            }

            class Button {
                companion object {
                    const val INTERACTION_ID = "interactId" // Key for the interaction ID of buttons
                    const val TEXT = "text" // Key for the text of buttons
                    const val ACTION_URL = "actionUrl" // Key for the action URL of buttons
                    const val BOR_WIDTH = "borWidth" // Key for the border width of buttons
                    const val BOR_COLOR = "borColor" // Key for the border color of buttons
                    const val BG_COLOR = "bgClr" // Key for the border color of buttons
                }
            }

            class Image {
                companion object {
                    const val URL = "url" // Key for the URL of the image
                    const val DARK_URL = "darkUrl" // Key for the URL of the image for dark mode
                    const val BUNDLE = "bundle" // Key for the image resource bundle
                    const val DARK_BUNDLE =
                        "darkBundle" // Key for the image resource bundle in dark mode
                    const val ICON = "icon" // Key for the icon name or identifier
                    const val ICON_SIZE = "iconSize" // Key for the size of the icon
                    const val ALT = "alt" // Key for text that describes the image
                    const val PLACEHOLDER = "placeholder" // Key for the placeholder image url
                }
            }

            class Font {
                companion object {
                    const val NAME = "name" // Key for the font name
                    const val SIZE = "size" // Key for the font size
                    const val WEIGHT = "weight" // Key for the font weight
                    const val STYLE = "style" // Key for the font style
                }
            }

            class Color {
                companion object {
                    const val LIGHT = "light" // Key for the light color
                    const val DARK = "dark" // Key for the dark color
                }
            }
        }
    }
}
