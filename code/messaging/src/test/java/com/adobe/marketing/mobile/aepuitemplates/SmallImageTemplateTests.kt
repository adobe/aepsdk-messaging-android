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

package com.adobe.marketing.mobile.aepuitemplates

import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPButton
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPDismissButton
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPImage
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.AEPText
import com.adobe.marketing.mobile.aepuitemplates.utils.AepUITemplateType
import com.adobe.marketing.mobile.messaging.UiTemplateConstructionFailedException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.BeforeTest

class SmallImageTemplateTests {
    lateinit var smallContentCardString: String

    @BeforeTest
    fun setup() {
        smallContentCardString = """
            {
                "actionUrl": "https://luma.com/sale",
                "title": {
                    "content": "Card Title"
                 },
                "body": {
                    "content": "body"
                },
                "image": {
                    "url": "https://imagetoDownload.com/cardimage",
                    "darkUrl": "https://imagetoDownload.com/darkimage"
                },
                "buttons": [
                    {
                        "interactId": "purchaseID",
                        "text": {
                            "content": "Purchase Now"
                        },
                        "actionUrl": "https://adobe.com/offer"
                    },
                    {
                        "interactId": "cancelID",
                        "text": {
                            "content": "Cancel"
                        },
                        "actionUrl": "app://home"
                    }
                ],
                "dismissBtn": {
                    "style": "circle"
                }
            }
        """
    }

    @Test
    fun test_SmallImageTemplate_initialization_fromJsonString() {
        // setup
        val template = SmallImageTemplate.fromJsonString(smallContentCardString)

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_initialization_fromParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            AEPText("body"),
            AEPImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AEPButton("purchaseID", AEPText("Purchase Now"), "https://adobe.com/offer"),
                AEPButton("cancelID", AEPText("Cancel"), "app://home")
            ),
            AEPDismissButton("circle")
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_getType() {
        // setup
        val template = SmallImageTemplate.fromJsonString(smallContentCardString)

        // verify
        assertEquals(AepUITemplateType.SMALL_IMAGE, template.getType())
    }

    @Test(expected = UiTemplateConstructionFailedException::class)
    fun test_SmallImageTemplate_missingTitleInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("title")
        SmallImageTemplate.fromJsonString(contentJson.toString())
    }

    @Test
    fun test_SmallImageTemplate_missingBodyInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("body")
        val template = SmallImageTemplate.fromJsonString(contentJson.toString())

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals(null, template.body)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingImageInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("image")
        val template = SmallImageTemplate.fromJsonString(contentJson.toString())

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals(null, template.image)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingActionUrlInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("actionUrl")
        val template = SmallImageTemplate.fromJsonString(contentJson.toString())

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals(null, template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingButtonsInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("buttons")
        val template = SmallImageTemplate.fromJsonString(contentJson.toString())

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(null, template.buttons)
    }

    @Test
    fun test_SmallImageTemplate_missingDismissButtonInJson() {
        // setup
        val contentJson = JSONObject(smallContentCardString)
        contentJson.remove("dismissBtn")
        val template = SmallImageTemplate.fromJsonString(contentJson.toString())

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(null, template.dismissBtn)
    }

    @Test
    fun test_SmallImageTemplate_missingBodyInParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            null,
            AEPImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AEPButton("purchaseID", AEPText("Purchase Now"), "https://adobe.com/offer"),
                AEPButton("cancelID", AEPText("Cancel"), "app://home")
            ),
            AEPDismissButton("circle")
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals(null, template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingImageInParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            AEPText("body"),
            null,
            "https://luma.com/sale",
            listOf(
                AEPButton("purchaseID", AEPText("Purchase Now"), "https://adobe.com/offer"),
                AEPButton("cancelID", AEPText("Cancel"), "app://home")
            ),
            AEPDismissButton("circle")
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals(null, template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingActionUrlInParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            AEPText("body"),
            AEPImage("https://imagetoDownload.com/cardimage"),
            null,
            listOf(
                AEPButton("purchaseID", AEPText("Purchase Now"), "https://adobe.com/offer"),
                AEPButton("cancelID", AEPText("Cancel"), "app://home")
            ),
            AEPDismissButton("circle")
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals(null, template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingButtonsInParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            AEPText("body"),
            AEPImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            null,
            AEPDismissButton("circle")
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(null, template.buttons)
        assertEquals("circle", template.dismissBtn?.style)
    }

    @Test
    fun test_SmallImageTemplate_missingDismissButtonInParameters() {
        // setup
        val template = SmallImageTemplate(
            AEPText("Card Title"),
            AEPText("body"),
            AEPImage("https://imagetoDownload.com/cardimage"),
            null,
            listOf(
                AEPButton("purchaseID", AEPText("Purchase Now"), "https://adobe.com/offer"),
                AEPButton("cancelID", AEPText("Cancel"), "app://home")
            ),
            null
        )

        // verify
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals(null, template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.interactId)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.interactId)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(null, template.dismissBtn)
    }
}
