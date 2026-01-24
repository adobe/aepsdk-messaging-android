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

import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertNull

class ImageOnlyTemplateTests {

    @Test
    fun `Get type should return IMAGE_ONLY template type`() {
        // setup
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://imagetoDownload.com/cardimage")
        )

        // verify
        assertEquals(AepUITemplateType.IMAGE_ONLY, template.getType())
    }

    @Test
    fun test_ImageOnlyTemplate_allParametersPresent() {
        // setup
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            AepIcon(1234),
            true
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("https://imagetoDownload.com/cardimage", template.image.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
        assertEquals(true, template.isRead)
    }

    @Test
    fun test_ImageOnlyTemplate_missingActionUrlInParameters() {
        // setup
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://imagetoDownload.com/cardimage"),
            null,
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("https://imagetoDownload.com/cardimage", template.image.url)
        assertEquals(null, template.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
    }

    @Test
    fun test_ImageOnlyTemplate_missingDismissButtonInParameters() {
        // setup
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            null
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("https://imagetoDownload.com/cardimage", template.image.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(null, template.dismissBtn)
    }

    @Test
    fun test_ImageOnlyTemplate_missingIsReadInParameters() {
        // setup
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://imagetoDownload.com/cardimage"),
            "https://luma.com/sale",
            AepIcon(1234)
        )

        // verify
        assertEquals("testId", template.id)
        assertEquals("https://imagetoDownload.com/cardimage", template.image.url)
        assertEquals("https://luma.com/sale", template.actionUrl)
        assertEquals(1234, template.dismissBtn?.drawableId)
        assertNull(template.isRead)
    }
}
