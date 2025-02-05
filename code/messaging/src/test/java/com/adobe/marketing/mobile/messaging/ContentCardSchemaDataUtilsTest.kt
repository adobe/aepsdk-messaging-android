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

import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.contentCardMap
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class ContentCardSchemaDataUtilsTest {

    @Rule
    @JvmField
    val exceptionRule: ExpectedException = ExpectedException.none()

    @Test
    fun `test createAepText with valid content`() {
        val titleMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] as? Map<String, Any>
        val result = ContentCardSchemaDataUtils.createAepText(titleMap ?: emptyMap())
        assertEquals("Messaging SDK Smoke Test", result.content)
    }

    @Test
    fun `test createAepText with missing content`() {
        exceptionRule.expect(NullPointerException::class.java)
        val titleMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] as? Map<String, Any>
        val map = titleMap?.toMutableMap()?.apply { remove(MessagingConstants.ContentCard.UIKeys.CONTENT) } ?: emptyMap()
        ContentCardSchemaDataUtils.createAepText(map)
    }

    @Test
    fun `test createAepImage with valid urls`() {
        val imageMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>
        val result = ContentCardSchemaDataUtils.createAepImage(imageMap ?: emptyMap())
        assertEquals("https://i.ibb.co/0X8R3TG/Messages-24.png", result.url)
        assertNull(result.darkUrl)
    }

    @Test
    fun `test createAepImage with missing urls`() {
        val imageMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>
        val map = imageMap?.toMutableMap()?.apply { remove(MessagingConstants.ContentCard.UIKeys.URL) } ?: emptyMap()
        val result = ContentCardSchemaDataUtils.createAepImage(map)
        assertNotNull(result)
        assertNull(result.url)
        assertNull(result.darkUrl)
    }

    @Test
    fun `test createAepButton with valid data`() {
        val buttons = contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] as? List<Map<String, Any>> ?: emptyList()
        val map = buttons[0]
        val result = ContentCardSchemaDataUtils.createAepButton(map)
        assertEquals("buttonID1", result.id)
        assertEquals("https://adobe.com/offer", result.actionUrl)
        assertEquals("Purchase Now", result.text.content)
    }

    @Test
    fun `test createAepButton with missing data`() {
        exceptionRule.expect(NullPointerException::class.java)
        val buttons = contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] as? List<Map<String, Any>> ?: emptyList()
        val map = buttons[0].toMutableMap().apply {
            remove(MessagingConstants.ContentCard.UIKeys.INTERACT_ID)
            remove(MessagingConstants.ContentCard.UIKeys.ACTION_URL)
            remove(MessagingConstants.ContentCard.UIKeys.TEXT)
        }
        ContentCardSchemaDataUtils.createAepButton(map)
    }

    @Test
    fun `test createAepDismissButton with simple style`() {
        val dismissMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>
        val result = ContentCardSchemaDataUtils.createAepDismissButton(dismissMap ?: emptyMap())
        assertNull(result)
    }

    @Test
    fun `test createAepDismissButton with circle style`() {
        val dismissMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>
        val map = dismissMap?.toMutableMap()?.apply {
            put(MessagingConstants.ContentCard.UIKeys.STYLE, MessagingConstants.ContentCard.UIKeys.CIRCLE)
        } ?: emptyMap()
        val result = ContentCardSchemaDataUtils.createAepDismissButton(map)
        assertNotNull(result)
        assertEquals(R.drawable.cancel_filled, result?.drawableId)
    }

    @Test
    fun `test createAepDismissButton with unsupported style`() {
        val dismissMap = contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>
        val map = dismissMap?.toMutableMap()?.apply {
            put(MessagingConstants.ContentCard.UIKeys.STYLE, "unsupported_style")
        } ?: emptyMap()
        val result = ContentCardSchemaDataUtils.createAepDismissButton(map)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with valid content card`() {
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(metaMap)

        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")

        contentCardSchemaData.parent = propositionItem

        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertTrue(result is SmallImageTemplate)
    }

    @Test
    fun `test getTemplate with missing title`() {
        exceptionRule.expect(NullPointerException::class.java)
        val contentMap = contentCardMap.toMutableMap().apply {
            remove(MessagingConstants.ContentCard.UIKeys.TITLE)
        }
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(contentCardSchemaData.parent.proposition.uniqueId).thenReturn("testId")
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(metaMap)
        ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
    }

    @Test
    fun `test getAepUI with SmallImageTemplate`() {
        val template = SmallImageTemplate("testId", AepText("Messaging SDK Smoke Test"), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template)
        assertNotNull(result)
    }

    @Test
    fun `test getAepUI with unsupported template type`() {
        val template = mock(AepUITemplate::class.java)
        val result = ContentCardSchemaDataUtils.getAepUI(template)
        assertNull(result)
    }

    @Test
    fun `test buildTemplate with valid proposition`() {
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(metaMap)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        Mockito.`when`(propositionItem.contentCardSchemaData).thenReturn(contentCardSchemaData)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        val result = ContentCardSchemaDataUtils.buildTemplate(proposition)
        assertNotNull(result)
        assertTrue(result is SmallImageTemplate)
    }

    @Test
    fun `test buildTemplate with empty proposition items`() {
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(emptyList())
        val result = ContentCardSchemaDataUtils.buildTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test isContentCard with valid schema`() {
        val propositionItem = mock(PropositionItem::class.java)
        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        val result = ContentCardSchemaDataUtils.isContentCard(proposition)
        assertTrue(result)
    }

    @Test
    fun `test isContentCard with invalid schema`() {
        val propositionItem = mock(PropositionItem::class.java)
        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.UNKNOWN)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        val result = ContentCardSchemaDataUtils.isContentCard(proposition)
        assertFalse(result)
    }
}
