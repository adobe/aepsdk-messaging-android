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

package com.adobe.marketing.mobile.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.lang.ref.SoftReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class ContentCardMapperTests {

    @Mock
    private lateinit var mockContentCardSchemaData: ContentCardSchemaData
    @Mock
    private lateinit var mockPropositionItem: PropositionItem
    @Mock
    private lateinit var mockProposition: Proposition

    @BeforeTest
    fun setup() {
        mockContentCardSchemaData = mock(ContentCardSchemaData::class.java)
        mockPropositionItem = mock(PropositionItem::class.java)
        mockProposition = mock(Proposition::class.java)

        mockContentCardSchemaData.parent = mockPropositionItem
        mockPropositionItem.propositionReference = SoftReference(mockProposition)
        `when`(mockPropositionItem.proposition).thenReturn(mockProposition)
        `when`(mockProposition.uniqueId).thenReturn("uniqueId")
    }

    @AfterTest
    fun tearDown() {
        ContentCardMapper.instance.clear()
        reset(mockContentCardSchemaData, mockPropositionItem, mockProposition)
    }

    @Test
    fun `Get content card schema data with valid id`() {
        val mapper = ContentCardMapper.instance
        mapper.storeContentCardSchemaData(mockContentCardSchemaData)

        val result = mapper.getContentCardSchemaData("uniqueId")
        assertEquals(mockContentCardSchemaData, result)
    }

    @Test
    fun `Get content card schema data with invalid id`() {
        val mapper = ContentCardMapper.instance
        mapper.storeContentCardSchemaData(mockContentCardSchemaData)

        val result = mapper.getContentCardSchemaData("invalidId")
        assertNull(result)
    }

    @Test
    fun `Get content card schema data with empty id`() {
        val mapper = ContentCardMapper.instance
        mapper.storeContentCardSchemaData(mockContentCardSchemaData)

        val result = mapper.getContentCardSchemaData("")
        assertNull(result)
    }

    @Test
    fun `Store content card schema data with null proposition reference`() {
        val mapper = ContentCardMapper.instance
        mockPropositionItem.propositionReference = null
        mockContentCardSchemaData.parent = mockPropositionItem

        mapper.storeContentCardSchemaData(mockContentCardSchemaData)

        assertNull(mapper.getContentCardSchemaData("uniqueId"))
    }

    @Test
    fun `Remove content card schema data with valid id`() {
        val mapper = ContentCardMapper.instance
        mapper.storeContentCardSchemaData(mockContentCardSchemaData)
        val result = mapper.getContentCardSchemaData("uniqueId")
        assertEquals(mockContentCardSchemaData, result)
        mapper.removeContentCardSchemaData("uniqueId")
        assertNull(mapper.getContentCardSchemaData("uniqueId"))
    }
}
