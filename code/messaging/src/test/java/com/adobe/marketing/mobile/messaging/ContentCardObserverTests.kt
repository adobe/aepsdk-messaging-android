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

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.Mockito
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
    private lateinit var mockSmallImageTemplateEventHandler: MockedConstruction<SmallImageTemplateEventHandler>

    @BeforeTest
    fun setup() {
        mockAepUI = mock(AepUI::class.java)
        mockSmallImageTemplate = mock(SmallImageTemplate::class.java)

        `when`(mockSmallImageTemplate.id).thenReturn("mockId")
        `when`(mockSmallImageTemplate.getType()).thenReturn(AepUITemplateType.SMALL_IMAGE)
        `when`(mockAepUI.getTemplate()).thenReturn(mockSmallImageTemplate)

        mockSmallImageTemplateEventHandler = Mockito.mockConstruction(SmallImageTemplateEventHandler::class.java)
    }

    @AfterTest
    fun tearDown() {
        reset(mockAepUI, mockSmallImageTemplate)
        mockSmallImageTemplateEventHandler.close()
    }

    @Test
    fun `Content card event observer receives a display event`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = ContentCardEventObserver(callback)
        val event = UIEvent.Display(mockAepUI)

        observer.onEvent(event)
        val expectedEvent = UIEvent.Display(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>)
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }

    @Test
    fun `Content card event observer receives a dismiss event`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = ContentCardEventObserver(callback)
        val event = UIEvent.Dismiss(mockAepUI)

        observer.onEvent(event)

        val expectedEvent = UIEvent.Dismiss(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>)
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }

    @Test
    fun `Content card event observer receives a click interact event`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = ContentCardEventObserver(callback)
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        val event = UIEvent.Interact(mockAepUI, action)

        observer.onEvent(event)

        val expectedEvent = UIEvent.Interact(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>, UIAction.Click(id = "button1", actionUrl = "http://example.com"))
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }
}
