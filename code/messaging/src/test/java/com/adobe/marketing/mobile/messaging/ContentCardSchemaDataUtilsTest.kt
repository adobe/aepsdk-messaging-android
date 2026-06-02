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

import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import com.adobe.marketing.mobile.services.DataStoring
import com.adobe.marketing.mobile.services.NamedCollection
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentCardSchemaDataUtilsTest {

    private lateinit var contentCardMap: MutableMap<String, Any>

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockDataStoring: DataStoring

    @Mock
    private lateinit var mockNamedCollection: NamedCollection

    @Before
    fun setup() {
        contentCardMap = ContentCardJsonDataUtils.contentCardMap.toMutableMap()
    }

    /**
     * Helper method to run tests with mocked ServiceProvider.
     */
    private fun runWithMockedServiceProvider(testBlock: () -> Unit) {
        mockServiceProvider = mock(ServiceProvider::class.java)
        mockDataStoring = mock(DataStoring::class.java)
        mockNamedCollection = mock(NamedCollection::class.java)

        mockStatic(ServiceProvider::class.java).use { serviceProviderMockedStatic: MockedStatic<ServiceProvider> ->
            serviceProviderMockedStatic.`when`<ServiceProvider> { ServiceProvider.getInstance() }
                .thenReturn(mockServiceProvider)
            `when`(mockServiceProvider.dataStoreService).thenReturn(mockDataStoring)
            `when`(mockDataStoring.getNamedCollection(anyString())).thenReturn(mockNamedCollection)

            testBlock()
        }
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
        assertEquals("https://i.ibb.co/0X8R3TG/Messages-dark-24.png", result?.darkUrl)
    }

    @Test
    fun `test createAepImage with darkUrl`() {
        val imageMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>)?.toMutableMap()
        imageMap?.set(MessagingConstants.ContentCard.UIKeys.DARK_URL, "dark.jpg")
        contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] = imageMap!!
        val result = ContentCardSchemaDataUtils.createAepImage(contentCardMap, "cardId")
        assertNotNull(result)
        assertEquals("https://i.ibb.co/0X8R3TG/Messages-24.png", result?.url)
        assertEquals("dark.jpg", result?.darkUrl)
    }

    @Test
    fun `test createAepImage with missing urls`() {
        val imageMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>)?.toMutableMap()
        imageMap?.remove(MessagingConstants.ContentCard.UIKeys.URL)
        imageMap?.remove(MessagingConstants.ContentCard.UIKeys.DARK_URL)
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
    fun `test createAepDismissButton with none style`() {
        val dismissMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] as? Map<String, Any>)?.toMutableMap()
        dismissMap?.put(MessagingConstants.ContentCard.UIKeys.STYLE, MessagingConstants.ContentCard.UIKeys.NONE)
        contentCardMap[MessagingConstants.ContentCard.UIKeys.DISMISS_BTN] = dismissMap!!
        val result = ContentCardSchemaDataUtils.createAepDismissButton(contentCardMap, "cardId")
        assertNull(result)
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")

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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with empty title`() {
        val titleMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] as? Map<String, Any>)?.toMutableMap()
        titleMap?.set(MessagingConstants.ContentCard.UIKeys.CONTENT, "")
        contentCardMap[MessagingConstants.ContentCard.UIKeys.TITLE] = titleMap!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(metaMap)
        Mockito.`when`(propositionItem.proposition).thenReturn(proposition)
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertTrue(result is LargeImageTemplate)
    }

    @Test
    fun `test getTemplate with large image template type and missing title`() {
        contentCardMap.remove(MessagingConstants.ContentCard.UIKeys.TITLE)
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with image only template type and blank image url`() {
        val imageMap = (contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] as? Map<String, Any>)?.toMutableMap()
        imageMap?.set(MessagingConstants.ContentCard.UIKeys.URL, "")
        contentCardMap[MessagingConstants.ContentCard.UIKeys.IMAGE] = imageMap!!

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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with content not a map`() {
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn("not a map")
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with null meta`() {
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(null)
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with missing adobe meta`() {
        val meta = metaMap.toMutableMap()
        meta.remove(MessagingConstants.ContentCard.UIKeys.ADOBE)
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getTemplate with missing template type`() {
        val meta = metaMap.toMutableMap()
        val adobeMeta = (meta[MessagingConstants.ContentCard.UIKeys.ADOBE] as? Map<String, Any>)?.toMutableMap()
        adobeMeta?.remove(MessagingConstants.ContentCard.UIKeys.TEMPLATE)
        meta[MessagingConstants.ContentCard.UIKeys.ADOBE] = adobeMeta!!
        val contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        Mockito.`when`(contentCardSchemaData.content).thenReturn(contentCardMap)
        Mockito.`when`(contentCardSchemaData.meta).thenReturn(meta)
        val result = ContentCardSchemaDataUtils.getTemplate(contentCardSchemaData)
        assertNull(result)
    }

    @Test
    fun `test getAepUI with SmallImageTemplate`() {
        val template = SmallImageTemplate("testId", AepText("Messaging SDK Smoke Test"), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template)
        assertNotNull(result)
        assertTrue(result is SmallImageUI)
        assertNull(result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with SmallImageTemplate and isRead true`() {
        val template = SmallImageTemplate("testId", AepText("Messaging SDK Smoke Test"), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template, true)
        assertNotNull(result)
        assertTrue(result is SmallImageUI)
        assertEquals(true, result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with SmallImageTemplate and isRead false`() {
        val template = SmallImageTemplate("testId", AepText("Messaging SDK Smoke Test"), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template, false)
        assertNotNull(result)
        assertTrue(result is SmallImageUI)
        assertEquals(false, result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with LargeImageTemplate`() {
        val template = LargeImageTemplate("testId", AepText("..."), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template)
        assertNotNull(result)
        assertTrue(result is LargeImageUI)
        assertNull(result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with LargeImageTemplate and isRead true`() {
        val template = LargeImageTemplate("testId", AepText("..."), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template, true)
        assertNotNull(result)
        assertTrue(result is LargeImageUI)
        assertEquals(true, result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with LargeImageTemplate and isRead false`() {
        val template = LargeImageTemplate("testId", AepText("..."), null, null, null, emptyList(), null)
        val result = ContentCardSchemaDataUtils.getAepUI(template, false)
        assertNotNull(result)
        assertTrue(result is LargeImageUI)
        assertEquals(false, result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with ImageOnlyTemplate`() {
        val template = ImageOnlyTemplate("testId", AepImage("http://..."))
        val result = ContentCardSchemaDataUtils.getAepUI(template)
        assertNotNull(result)
        assertTrue(result is ImageOnlyUI)
        assertNull(result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with ImageOnlyTemplate and isRead true`() {
        val template = ImageOnlyTemplate("testId", AepImage("http://..."))
        val result = ContentCardSchemaDataUtils.getAepUI(template, true)
        assertNotNull(result)
        assertTrue(result is ImageOnlyUI)
        assertEquals(true, result?.getState()?.read)
    }

    @Test
    fun `test getAepUI with ImageOnlyTemplate and isRead false`() {
        val template = ImageOnlyTemplate("testId", AepImage("http://..."))
        val result = ContentCardSchemaDataUtils.getAepUI(template, false)
        assertNotNull(result)
        assertTrue(result is ImageOnlyUI)
        assertEquals(false, result?.getState()?.read)
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
        Mockito.`when`(proposition.getActivityId()).thenReturn("testId")
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
    fun `test buildTemplate with non content card proposition`() {
        val proposition = mock(Proposition::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        val result = ContentCardSchemaDataUtils.buildTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test buildTemplate with null contentCardSchemaData`() {
        val propositionItem = mock(PropositionItem::class.java)
        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        Mockito.`when`(propositionItem.contentCardSchemaData).thenReturn(null)
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))

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
    fun `test isContentCard with empty items`() {
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(emptyList())
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

    @Test
    fun `test getActionUrl with blank actionUrl`() {
        contentCardMap[MessagingConstants.ContentCard.UIKeys.ACTION_URL] = ""
        val result = ContentCardSchemaDataUtils.getActionUrl(contentCardMap, "cardId")
        assertEquals("", result)
    }

    // Tests for setReadStatus and getReadStatus

    @Test
    fun `test setReadStatus with valid activityId sets read status to true`() {
        runWithMockedServiceProvider {
            ContentCardSchemaDataUtils.setReadStatus("testActivityId", true)

            verify(mockNamedCollection).setBoolean("testActivityId", true)
        }
    }

    @Test
    fun `test setReadStatus with valid activityId sets read status to false`() {
        runWithMockedServiceProvider {
            ContentCardSchemaDataUtils.setReadStatus("testActivityId", false)

            verify(mockNamedCollection).setBoolean("testActivityId", false)
        }
    }

    @Test
    fun `test setReadStatus with null collection does not throw`() {
        mockServiceProvider = mock(ServiceProvider::class.java)
        mockDataStoring = mock(DataStoring::class.java)

        mockStatic(ServiceProvider::class.java).use { serviceProviderMockedStatic: MockedStatic<ServiceProvider> ->
            serviceProviderMockedStatic.`when`<ServiceProvider> { ServiceProvider.getInstance() }
                .thenReturn(mockServiceProvider)
            `when`(mockServiceProvider.dataStoreService).thenReturn(mockDataStoring)
            `when`(mockDataStoring.getNamedCollection(anyString())).thenReturn(null)

            // Should not throw exception
            ContentCardSchemaDataUtils.setReadStatus("testActivityId", true)
        }
    }

    @Test
    fun `test getReadStatus returns true when collection returns true`() {
        runWithMockedServiceProvider {
            `when`(mockNamedCollection.getBoolean("testActivityId", false)).thenReturn(true)

            val result = ContentCardSchemaDataUtils.getReadStatus("testActivityId")

            assertEquals(true, result)
            verify(mockNamedCollection).getBoolean("testActivityId", false)
        }
    }

    @Test
    fun `test getReadStatus returns false when collection returns false`() {
        runWithMockedServiceProvider {
            `when`(mockNamedCollection.getBoolean("testActivityId", false)).thenReturn(false)

            val result = ContentCardSchemaDataUtils.getReadStatus("testActivityId")

            assertEquals(false, result)
            verify(mockNamedCollection).getBoolean("testActivityId", false)
        }
    }

    @Test
    fun `test getReadStatus returns null when collection is null`() {
        mockServiceProvider = mock(ServiceProvider::class.java)
        mockDataStoring = mock(DataStoring::class.java)

        mockStatic(ServiceProvider::class.java).use { serviceProviderMockedStatic: MockedStatic<ServiceProvider> ->
            serviceProviderMockedStatic.`when`<ServiceProvider> { ServiceProvider.getInstance() }
                .thenReturn(mockServiceProvider)
            `when`(mockServiceProvider.dataStoreService).thenReturn(mockDataStoring)
            `when`(mockDataStoring.getNamedCollection(anyString())).thenReturn(null)

            val result = ContentCardSchemaDataUtils.getReadStatus("testActivityId")

            assertNull(result)
        }
    }

    @Test
    fun `test getReadStatus with different activityIds`() {
        runWithMockedServiceProvider {
            `when`(mockNamedCollection.getBoolean("activity1", false)).thenReturn(true)
            `when`(mockNamedCollection.getBoolean("activity2", false)).thenReturn(false)

            assertEquals(true, ContentCardSchemaDataUtils.getReadStatus("activity1"))
            assertEquals(false, ContentCardSchemaDataUtils.getReadStatus("activity2"))
        }
    }

    // Tests for createAlignment
    @Test
    fun `test createAlignment with top left`() {
        val result = ContentCardSchemaDataUtils.createAlignment("topleft")
        assertEquals(androidx.compose.ui.Alignment.TopStart, result)
    }

    @Test
    fun `test createAlignment with top left uppercase`() {
        val result = ContentCardSchemaDataUtils.createAlignment("TOPLEFT")
        assertEquals(androidx.compose.ui.Alignment.TopStart, result)
    }

    @Test
    fun `test createAlignment with top right`() {
        val result = ContentCardSchemaDataUtils.createAlignment("topright")
        assertEquals(androidx.compose.ui.Alignment.TopEnd, result)
    }

    @Test
    fun `test createAlignment with bottom left`() {
        val result = ContentCardSchemaDataUtils.createAlignment("bottomleft")
        assertEquals(androidx.compose.ui.Alignment.BottomStart, result)
    }

    @Test
    fun `test createAlignment with bottom right`() {
        val result = ContentCardSchemaDataUtils.createAlignment("bottomright")
        assertEquals(androidx.compose.ui.Alignment.BottomEnd, result)
    }

    @Test
    fun `test createAlignment with invalid value`() {
        val result = ContentCardSchemaDataUtils.createAlignment("invalid")
        assertNull(result)
    }

    @Test
    fun `test createAlignment with null value`() {
        val result = ContentCardSchemaDataUtils.createAlignment(null)
        assertNull(result)
    }

    // Tests for createAepColor
    @Test
    fun `test createAepColor with valid light color only`() {
        val colorMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LIGHT to "#FF0000FF"
        )
        val result = ContentCardSchemaDataUtils.createAepColor(colorMap, "inboxId")
        assertNotNull(result)
        assertEquals(androidx.compose.ui.graphics.Color.Red, result?.light)
        assertNull(result?.dark)
    }

    @Test
    fun `test createAepColor with light and dark colors`() {
        val colorMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LIGHT to "#FF0000FF",
            MessagingConstants.Inbox.UIKeys.DARK to "#00FF00FF"
        )
        val result = ContentCardSchemaDataUtils.createAepColor(colorMap, "inboxId")
        assertNotNull(result)
        assertEquals(androidx.compose.ui.graphics.Color.Red, result?.light)
        assertEquals(androidx.compose.ui.graphics.Color.Green, result?.dark)
    }

    @Test
    fun `test createAepColor with missing light color`() {
        val colorMap = mapOf(
            MessagingConstants.Inbox.UIKeys.DARK to "#FF00FF00"
        )
        val result = ContentCardSchemaDataUtils.createAepColor(colorMap, "inboxId")
        assertNull(result)
    }

    @Test
    fun `test createAepColor with null light color`() {
        val colorMap = emptyMap<String, Any>()
        val result = ContentCardSchemaDataUtils.createAepColor(colorMap, "inboxId")
        assertNull(result)
    }

    // Tests for createAepInboxLayout
    @Test
    fun `test createAepInboxLayout with valid vertical orientation`() {
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val inboxMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNotNull(result)
        assertEquals(com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout.VERTICAL, result)
    }

    @Test
    fun `test createAepInboxLayout with valid horizontal orientation`() {
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "horizontal"
        )
        val inboxMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNotNull(result)
        assertEquals(com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout.HORIZONTAL, result)
    }

    @Test
    fun `test createAepInboxLayout with invalid orientation`() {
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "invalid"
        )
        val inboxMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNull(result)
    }

    @Test
    fun `test createAepInboxLayout with missing layout key`() {
        val inboxMap = emptyMap<String, Any>()
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNull(result)
    }

    @Test
    fun `test createAepInboxLayout with empty layout map`() {
        val layoutMap = emptyMap<String, Any>()
        val inboxMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNull(result)
    }

    @Test
    fun `test createAepInboxLayout with missing orientation`() {
        val layoutMap = mapOf<String, Any>()
        val inboxMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )
        val result = ContentCardSchemaDataUtils.createAepInboxLayout(inboxMap, "inboxId")
        assertNull(result)
    }

    // Tests for createInboxTemplate

    @Test
    fun `test createInboxTemplate with valid data`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNotNull(result)
        assertEquals("inboxId", result?.id)
        assertEquals("My Inbox", result?.heading?.content)
        assertNotNull(result?.layout)
        assertEquals(10, result?.capacity)
    }

    @Test
    fun `test createInboxTemplate with empty state settings`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val emptyMessageMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "No messages"
        )
        val emptyImageMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.URL to "https://example.com/empty.png"
        )
        val emptyStateSettings = mapOf(
            MessagingConstants.Inbox.UIKeys.MESSAGE to emptyMessageMap,
            MessagingConstants.ContentCard.UIKeys.IMAGE to emptyImageMap
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10,
            MessagingConstants.Inbox.UIKeys.EMPTY_STATE_SETTINGS to emptyStateSettings
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNotNull(result)
        assertEquals("No messages", result?.emptyMessage?.content)
        assertEquals("https://example.com/empty.png", result?.emptyImage?.url)
    }

    @Test
    fun `test createInboxTemplate with unread indicator settings`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "horizontal"
        )
        val unreadIconMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.IMAGE to mapOf(MessagingConstants.ContentCard.UIKeys.URL to "https://example.com/unread.png")
        )
        val unreadBgColorMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LIGHT to "#FFFF0000"
        )
        val unreadBgMap = mapOf(
            MessagingConstants.Inbox.UIKeys.CLR to unreadBgColorMap
        )
        val unreadIndicator = mapOf(
            MessagingConstants.Inbox.UIKeys.UNREAD_ICON to unreadIconMap,
            MessagingConstants.Inbox.UIKeys.UNREAD_BG to unreadBgMap,
            MessagingConstants.Inbox.UIKeys.PLACEMENT to "topleft"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10,
            MessagingConstants.Inbox.UIKeys.IS_UNREAD_ENABLED to true,
            MessagingConstants.Inbox.UIKeys.UNREAD_INDICATOR to unreadIndicator
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNotNull(result)
        assertEquals(true, result?.isUnreadEnabled)
        assertEquals("https://example.com/unread.png", result?.unreadIcon?.url)
        assertNotNull(result?.unreadBgColor)
        assertEquals(androidx.compose.ui.Alignment.TopStart, result?.unreadIconAlignment)
    }

    @Test
    fun `test createInboxTemplate with null proposition items`() {
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(emptyList())

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with empty content map`() {
        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(emptyMap())
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with missing heading`() {
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with empty heading content`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to ""
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with missing layout`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with missing capacity`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with zero capacity`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 0
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with negative capacity`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to -1
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with default isUnreadEnabled false`() {
        val headingMap = mapOf(
            MessagingConstants.ContentCard.UIKeys.CONTENT to "My Inbox"
        )
        val layoutMap = mapOf(
            MessagingConstants.Inbox.UIKeys.ORIENTATION to "vertical"
        )
        val contentMap = mapOf(
            MessagingConstants.Inbox.UIKeys.HEADING to headingMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT to layoutMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY to 10
        )

        val inboxSchemaData = mock(InboxContentSchemaData::class.java)
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(inboxSchemaData)
        Mockito.`when`(inboxSchemaData.content).thenReturn(contentMap)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNotNull(result)
        assertEquals(false, result?.isUnreadEnabled)
    }

    @Test
    fun `test createInboxTemplate with non-inbox schema`() {
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    @Test
    fun `test createInboxTemplate with null inboxSchemaData`() {
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(propositionItem.inboxSchemaData).thenReturn(null)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))
        Mockito.`when`(proposition.activityId).thenReturn("inboxId")

        val result = ContentCardSchemaDataUtils.createInboxTemplate(proposition)
        assertNull(result)
    }

    // Tests for isInbox
    @Test
    fun `test isInbox with valid inbox proposition`() {
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.INBOX)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))

        val result = ContentCardSchemaDataUtils.isInbox(proposition)
        assertTrue(result)
    }

    @Test
    fun `test isInbox with non-inbox schema`() {
        val propositionItem = mock(PropositionItem::class.java)
        val proposition = mock(Proposition::class.java)

        Mockito.`when`(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        Mockito.`when`(proposition.items).thenReturn(listOf(propositionItem))

        val result = ContentCardSchemaDataUtils.isInbox(proposition)
        assertFalse(result)
    }

    @Test
    fun `test isInbox with empty items`() {
        val proposition = mock(Proposition::class.java)
        Mockito.`when`(proposition.items).thenReturn(emptyList())

        val result = ContentCardSchemaDataUtils.isInbox(proposition)
        assertFalse(result)
    }

    // Tests for toComposeColor
    @Test
    fun `test toComposeColor with 6-char RGB hex returns opaque color`() {
        assertEquals(androidx.compose.ui.graphics.Color.Red, "#FF0000".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 6-char RGB hex green`() {
        assertEquals(androidx.compose.ui.graphics.Color.Green, "#00FF00".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 6-char RGB hex blue`() {
        assertEquals(androidx.compose.ui.graphics.Color.Blue, "#0000FF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 6-char RGB hex black`() {
        assertEquals(androidx.compose.ui.graphics.Color.Black, "#000000".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 6-char RGB hex white`() {
        assertEquals(androidx.compose.ui.graphics.Color.White, "#FFFFFF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 8-char RRGGBBAA hex fully opaque red`() {
        assertEquals(androidx.compose.ui.graphics.Color.Red, "#FF0000FF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 8-char RRGGBBAA hex fully opaque green`() {
        assertEquals(androidx.compose.ui.graphics.Color.Green, "#00FF00FF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 8-char RRGGBBAA hex zero alpha`() {
        val result = "#FF000000".toComposeColor()
        assertNotNull(result)
        assertEquals(androidx.compose.ui.graphics.Color(255, 0, 0, 0), result)
    }

    @Test
    fun `test toComposeColor with 8-char RRGGBBAA hex semi-transparent`() {
        val result = "#FF000080".toComposeColor()
        assertNotNull(result)
        assertEquals(androidx.compose.ui.graphics.Color(255, 0, 0, 128), result)
    }

    @Test
    fun `test toComposeColor with 5-char hex returns null`() {
        assertNull("#FFFFF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with 7-char hex returns null`() {
        assertNull("#FFFFFFF".toComposeColor())
    }

    @Test
    fun `test toComposeColor with empty string returns null`() {
        assertNull("".toComposeColor())
    }

    @Test
    fun `test toComposeColor with only hash returns null`() {
        assertNull("#".toComposeColor())
    }

    @Test
    fun `test toComposeColor with lowercase hex is parsed correctly`() {
        assertEquals(androidx.compose.ui.graphics.Color.Red, "#ff0000".toComposeColor())
    }

    @Test
    fun `test toComposeColor with invalid hex characters returns null`() {
        assertNull("#GGGGGG".toComposeColor())
    }

    @Test
    fun `test toComposeColor with invalid 8-char hex characters returns null`() {
        assertNull("#FF0000ZZ".toComposeColor())
    }
}
