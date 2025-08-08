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
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class ContentCardSchemaDataUtilsTest {

    private lateinit var contentCardMap: MutableMap<String, Any>

    @Before
    fun setup() {
        contentCardMap = ContentCardJsonDataUtils.contentCardMap.toMutableMap()
    }

    @Test
    fun `test createAepText with valid content`() {
        val result = ContentCardSchemaDataUtils.createAepText(contentCardMap, MessagingConstants.ContentCard.UIKeys.TITLE, "cardId")
        assertEquals("Messaging SDK Smoke Test", result?.content)
    }

    @Test
    fun `test createAepText with missing content`() {
        val titleMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] as? Map<*, *>)?.toMutableMap()
        titleMap?.remove(MessagingConstants.ContentCard.UIKeys.CONTENT)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] = titleMap!!
        val result = ContentCardSchemaDataUtils.createAepText(contentCardMap, MessagingConstants.ContentCard.UIKeys.TITLE, "cardId")
        assertNull(result)
    }

    @Test
    fun `test createAepText with missing text key`() {
        val result = ContentCardSchemaDataUtils.createAepText(contentCardMap, "invalidKey", "cardId")
        assertNull(result)
    }

    @Test
    fun `test createAepImage with valid urls`() {
        val result = ContentCardSchemaDataUtils.createAepImage(contentCardMap, "cardId")
        assertEquals("https://i.ibb.co/0X8R3TG/Messages-24.png", result?.url)
        assertNull(result?.darkUrl)
    }

    @Test
    fun `test createAepImage with missing urls`() {
        val imageMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>)?.toMutableMap()
        imageMap?.remove(MessagingConstants.ContentCard.UIKeys.URL)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] = imageMap!!
        val result = ContentCardSchemaDataUtils.createAepImage(contentCardMap, "cardId")
        assertNotNull(result)
        assertNull(result?.url)
        assertNull(result?.darkUrl)
    }

    @Test
    fun `test createAepImage with missing image key`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.IMAGE)
        val result = ContentCardSchemaDataUtils.createAepImage(contentCardMap, "cardId")
        assertNull(result)
    }

    @Test
    fun `test createAepButtonsList with valid data`() {
        val result = ContentCardSchemaDataUtils.createAepButtonsList(contentCardMap, "cardId")
        assertEquals(1, result?.size)
        assertEquals("buttonID1", result?.get(0)?.id)
        assertEquals("https://adobe.com/offer", result?.get(0)?.actionUrl)
        assertEquals("Purchase Now", result?.get(0)?.text?.content)
    }

    @Test
    fun `test createAepButtonsList with missing data`() {
        val buttons = (contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] as? List<Map<String, Any>>)?.toMutableList()
        val button = buttons?.get(0)?.toMutableMap()
        button?.remove(MessagingConstants.ContentCard.UIKeys.INTERACT_ID)
        button?.remove(MessagingConstants.ContentCard.UIKeys.ACTION_URL)
        button?.remove(MessagingConstants.ContentCard.UIKeys.TEXT)
        buttons?.set(0, button!!)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] = buttons!!
        val result = ContentCardSchemaDataUtils.createAepButtonsList(contentCardMap, "cardId")
        assertTrue(result?.isEmpty() ?: true)
    }

    @Test
    fun `test createAepButtonsList with one valid and one invalid button`() {
        val buttons = (contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] as? List<Map<String, Any>>)?.toMutableList()
        val invalidButton = mutableMapOf<String, Any>()
        buttons?.add(invalidButton)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.BUTTONS] = buttons!!
        val result = ContentCardSchemaDataUtils.createAepButtonsList(contentCardMap, "cardId")
        assertEquals(1, result?.size)
    }

    @Test
    fun `test createAepButtonsList with no buttons`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.BUTTONS)
        val result = ContentCardSchemaDataUtils.createAepButtonsList(contentCardMap, "cardId")
        assertNull(result)
    }

    @Test
    fun `test createAepDismissButton with simple style`() {
        val dismissMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>)?.toMutableMap()
        dismissMap?.set(MessagingConstants.ContentCard.UIKeys.STYLE, "simple")
        contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] = dismissMap!!
        val result = ContentCardSchemaDataUtils.createAepDismissButton(contentCardMap, "cardId")
        assertNotNull(result)
        assertEquals(R.drawable.close_filled, result?.drawableId)
    }

    @Test
    fun `test createAepDismissButton with circle style`() {
        val dismissMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>)?.toMutableMap()
        dismissMap?.put(MessagingConstants.ContentCard.UIKeys.STYLE, MessagingConstants.ContentCard.UIKeys.CIRCLE)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] = dismissMap!!
        val result = ContentCardSchemaDataUtils.createAepDismissButton(contentCardMap, "cardId")
        assertNotNull(result)
        assertEquals(R.drawable.cancel_filled, result?.drawableId)
    }

    @Test
    fun `test createAepDismissButton with unsupported style`() {
        val dismissMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>)?.toMutableMap()
        dismissMap?.put(MessagingConstants.ContentCard.UIKeys.STYLE, "unsupported_style")
        contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] = dismissMap!!
        val result = ContentCardSchemaDataUtils.createAepDismissButton(contentCardMap, "cardId")
        assertNull(result)
    }

    @Test
    fun `test createAepDismissButton with missing dismiss button key`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.DISMISS_BTN)
        val result = ContentCardSchemaDataUtils.createAepDismissButton(contentCardMap, "cardId")
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
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.TITLE)
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(metaMap)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with large image template type`() {
        val meta = metaMap.toMutableMap()
        val adobeMeta = (meta[MessagingConstants.ContentCard.UIKeys.ADOBE] as? Map<String, Any>)?.toMutableMap()
        adobeMeta?.set(MessagingConstants.ContentCard.UIKeys.TEMPLATE, "LargeImage")
        meta[MessagingConstants.ContentCard.UIKeys.ADOBE] = adobeMeta!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertTrue(result is LargeImageTemplate)
    }

    @Test
    fun `test getTemplate with image only template type`() {
        val meta = metaMap.toMutableMap()
        val adobeMeta = (meta[MessagingConstants.ContentCard.UIKeys.ADOBE] as? Map<String, Any>)?.toMutableMap()
        adobeMeta?.set(MessagingConstants.ContentCard.UIKeys.TEMPLATE, "ImageOnly")
        meta[MessagingConstants.ContentCard.UIKeys.ADOBE] = adobeMeta!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertTrue(result is ImageOnlyTemplate)
    }

    @Test
    fun `test getTemplate with image only template type and missing image`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.IMAGE)
        val meta = metaMap.toMutableMap()
        val adobeMeta = (meta[MessagingConstants.ContentCard.UIKeys.ADOBE] as? Map<String, Any>)?.toMutableMap()
        adobeMeta?.set(MessagingConstants.ContentCard.UIKeys.TEMPLATE, "ImageOnly")
        meta[MessagingConstants.ContentCard.UIKeys.ADOBE] = adobeMeta!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with unsupported template type`() {
        val meta = metaMap.toMutableMap()
        val adobeMeta = (meta[MessagingConstants.ContentCard.UIKeys.ADOBE] as? Map<String, Any>)?.toMutableMap()
        adobeMeta?.set(MessagingConstants.ContentCard.UIKeys.TEMPLATE, "unsupported")
        meta[MessagingConstants.ContentCard.UIKeys.ADOBE] = adobeMeta!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
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

    @Test
    fun `test getActionUrl with valid actionUrl`() {
        contentCardMap[MessagingConstants.ContentCard.UIKeys.ACTION_URL] = "https://adobe.com"
        val result = ContentCardSchemaDataUtils.getActionUrl(contentCardMap, "cardId")
        assertEquals("https://adobe.com", result)
    }

    @Test
    fun `test getActionUrl with missing actionUrl`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.ACTION_URL)
        val result = ContentCardSchemaDataUtils.getActionUrl(contentCardMap, "cardId")
        assertNull(result)
    }
}
