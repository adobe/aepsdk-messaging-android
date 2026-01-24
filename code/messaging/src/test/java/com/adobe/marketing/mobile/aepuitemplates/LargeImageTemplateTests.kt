/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepuitemplates

import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertNull

class LargeImageTemplateTests {

    @Test
    fun `Get type should return LARGE_IMAGE template type`() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title")
        )

        // verify
        assertEquals(AepUITemplateType.LARGE_IMAGE, template.getType())
    }

    @Test
    fun test_LargeImageTemplate_allParametersPresent() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            AepIcon(1234),
            true
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
        assertEquals(true, template.isRead)
    }

    @Test
    fun test_LargeImageTemplate_missingBodyInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            null,
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals(null, template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
    }

    @Test
    fun test_LargeImageTemplate_missingImageInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            null,
            "https://luma.com/sale",
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals(null, template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
    }

    @Test
    fun test_LargeImageTemplate_missingActionUrlInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            AepImage("https://imagetoDownload.com/cardimage"),
            null,
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals(null, template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
    }

    @Test
    fun test_LargeImageTemplate_missingButtonsInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            null,
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(null, template.buttons)
        assertEquals(1234, template.dismissBtn?.drawableId)
    }

    @Test
    fun test_LargeImageTemplate_missingDismissButtonInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            null
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(null, template.dismissBtn)
    }

    @Test
    fun test_LargeImageTemplate_missingIsReadInParameters() {
        // setup
        val template = LargeImageTemplate(
            "testId",
            AepText("Card Title"),
            AepText("body"),
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            listOf(
                AepButton("purchaseID", "https://adobe.com/offer", AepText("Purchase Now")),
                AepButton("cancelID", "app://home", AepText("Cancel"))
            ),
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("Card Title", template.title.content)
        assertEquals("body", template.body?.content)
        assertEquals("https://imagetoDownload.com/cardimage", template.image?.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        val buttons = template.buttons
        assertEquals(2, buttons?.size)
        assertEquals("purchaseID", buttons?.get(0)?.id)
        assertEquals("Purchase Now", buttons?.get(0)?.text?.content)
        assertEquals("https://adobe.com/offer", buttons?.get(0)?.actionUrl)
        assertEquals("cancelID", buttons?.get(1)?.id)
        assertEquals("Cancel", buttons?.get(1)?.text?.content)
        assertEquals("app://home", buttons?.get(1)?.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
        assertNull(template.isRead)
    }
}
