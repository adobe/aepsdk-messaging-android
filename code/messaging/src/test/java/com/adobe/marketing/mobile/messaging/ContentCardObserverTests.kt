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

import com.adobe.marketing.mobile.MessagingEdgeEventType
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class ContentCardObserverTests {

    @Mock
    private lateinit var mockAepUI: AepUI<*, *>
    @Mock
    private lateinit var mockSmallImageTemplate: SmallImageTemplate
    @Mock
    private lateinit var mockContentCardMapper: ContentCardMapper
    @Mock
    private lateinit var mockContentCardSchemaData: ContentCardSchemaData

    @BeforeTest
    fun setup() {
        mockAepUI = mock(AepUI::class.java)
        mockSmallImageTemplate = mock(SmallImageTemplate::class.java)
        mockContentCardMapper = mock(ContentCardMapper::class.java)
        mockContentCardSchemaData = mock(ContentCardSchemaData::class.java)

        `when`(mockContentCardMapper.getContentCardSchemaData(anyString())).thenReturn(mockContentCardSchemaData)
        `when`(mockSmallImageTemplate.id).thenReturn("mockId")
        `when`(mockSmallImageTemplate.getType()).thenReturn(AepUITemplateType.SMALL_IMAGE)
        `when`(mockAepUI.getTemplate()).thenReturn(mockSmallImageTemplate)

        mockkObject(ContentCardMapper)
        every { ContentCardMapper.instance } returns mockContentCardMapper
    }

    @AfterTest
    fun tearDown() {
        reset(mockAepUI, mockSmallImageTemplate, mockContentCardMapper, mockContentCardSchemaData)
    }

    @Test
    fun `Content card event observer receives a display event`() {
        val callback = mock(ContentCardUIEventListening::class.java)
        val observer = ContentCardEventObserver(callback)
        val event = UIEvent.Display(mockAepUI)

        observer.onEvent(event)

        verify(callback, times(1)).onDisplay(mockAepUI)
        verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `Content card event observer receives a dismiss event`() {
        val callback = mock(ContentCardUIEventListening::class.java)
        val observer = ContentCardEventObserver(callback)
        val event = UIEvent.Dismiss(mockAepUI)

        observer.onEvent(event)

        verify(callback, times(1)).onDismiss(mockAepUI)
        verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISMISS)
    }

    @Test
    fun `Content card event observer receives a click interact event`() {
        val callback = mock(ContentCardUIEventListening::class.java)
        val observer = ContentCardEventObserver(callback)
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        val event = UIEvent.Interact(mockAepUI, action)

        observer.onEvent(event)

        verify(callback, times(1)).onInteract(mockAepUI, "button1", "http://example.com")
        verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
    }

    @Test
    fun `Content card event observer receives an interact event with no callback provided`() {
        val observer = ContentCardEventObserver(null)
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        val event = UIEvent.Interact(mockAepUI, action)

        observer.onEvent(event)

        // verify that the track is still called
        verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
    }
}
