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

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentCardAepUIExtensionsTests {
    private lateinit var mockContentCardMapper: ContentCardMapper
    private val mockAepUI = mockk<AepUI<*, *>>(relaxed = true)
    private val mockSmallImageTemplate = mockk<SmallImageTemplate>()
    private val mockLargeImageTemplate = mockk<LargeImageTemplate>()
    private val mockImageOnlyTemplate = mockk<ImageOnlyTemplate>()
    private val mockUnsupportedTemplate = mockk<AepUITemplate>()
    private val mockSchemaData = mockk<ContentCardSchemaData>()
    private val metadata = mapOf("key" to "value")

    @BeforeTest
    fun setup() {
        mockkObject(ContentCardMapper)
        mockContentCardMapper = mockk(relaxed = true)
        every { ContentCardMapper.instance } returns mockContentCardMapper
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(ContentCardMapper)
        clearAllMocks()
    }

    @Test
    fun `getMeta with SmallImageTemplate returns metadata`() {
        // setup
        every { mockSmallImageTemplate.id } returns "test-id"
        every { mockAepUI.getTemplate() } returns mockSmallImageTemplate
        every { mockSchemaData.meta } returns metadata
        every { mockContentCardMapper.getContentCardSchemaData("test-id") } returns mockSchemaData

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertEquals(metadata, result)
    }

    @Test
    fun `getMeta with LargeImageTemplate returns metadata`() {
        // setup
        every { mockLargeImageTemplate.id } returns "test-id"
        every { mockAepUI.getTemplate() } returns mockLargeImageTemplate
        every { mockSchemaData.meta } returns metadata
        every { mockContentCardMapper.getContentCardSchemaData("test-id") } returns mockSchemaData

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertEquals(metadata, result)
    }

    @Test
    fun `getMeta with ImageOnlyTemplate returns metadata`() {
        // setup
        every { mockImageOnlyTemplate.id } returns "test-id"
        every { mockAepUI.getTemplate() } returns mockImageOnlyTemplate
        every { mockSchemaData.getMeta() } returns metadata
        every { mockContentCardMapper.getContentCardSchemaData("test-id") } returns mockSchemaData

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertEquals(metadata, result)
    }

    @Test
    fun `getMeta with unsupported template returns null`() {
        // setup
        every { mockAepUI.getTemplate() } returns mockUnsupportedTemplate

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertNull(result)
    }

    @Test
    fun `getMeta returns null when schema data not found`() {
        // setup
        every { mockSmallImageTemplate.id } returns "test-id"
        every { mockAepUI.getTemplate() } returns mockSmallImageTemplate
        every { mockContentCardMapper.getContentCardSchemaData("test-id") } returns null

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertNull(result)
    }

    @Test
    fun `getMeta returns null when metadata is null in schema data`() {
        // setup
        every { mockSmallImageTemplate.id } returns "test-id"
        every { mockAepUI.getTemplate() } returns mockSmallImageTemplate
        every { mockSchemaData.getMeta() } returns null
        every { mockContentCardMapper.getContentCardSchemaData("test-id") } returns mockSchemaData

        // test
        val result = mockAepUI.getMeta()

        // verify
        assertNull(result)
    }
}
