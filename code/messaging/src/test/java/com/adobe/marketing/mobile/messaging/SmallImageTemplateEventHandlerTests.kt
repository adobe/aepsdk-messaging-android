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
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.uri.UriOpening
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class SmallImageTemplateEventHandlerTests {

    @Mock
    private lateinit var mockAepUI: AepUI<*, *>
    @Mock
    private lateinit var mockSmallImageTemplate: SmallImageTemplate
    @Mock
    private lateinit var mockServiceProvider: ServiceProvider
    @Mock
    private lateinit var mockUriOpening: UriOpening
    @Mock
    private lateinit var mockContentCardMapper: ContentCardMapper
    @Mock
    private lateinit var mockContentCardSchemaData: ContentCardSchemaData

    @BeforeTest
    fun setup() {
        mockAepUI = mock(AepUI::class.java)
        mockSmallImageTemplate = mock(SmallImageTemplate::class.java)
        mockServiceProvider = mock(ServiceProvider::class.java)
        mockUriOpening = mock(UriOpening::class.java)
        mockContentCardMapper = mock(ContentCardMapper::class.java)
        mockContentCardSchemaData = mock(ContentCardSchemaData::class.java)

        `when`(mockContentCardMapper.getContentCardSchemaData(anyString())).thenReturn(mockContentCardSchemaData)
        `when`(mockServiceProvider.uriService).thenReturn(mockUriOpening)
        `when`(mockSmallImageTemplate.id).thenReturn("mockId")
        `when`(mockSmallImageTemplate.getType()).thenReturn(AepUITemplateType.SMALL_IMAGE)
        `when`(mockAepUI.getTemplate()).thenReturn(mockSmallImageTemplate)

        mockkObject(ContentCardMapper)
        every { ContentCardMapper.instance } returns mockContentCardMapper
    }

    @AfterTest
    fun tearDown() {
        reset(mockAepUI, mockSmallImageTemplate, mockServiceProvider, mockUriOpening, mockContentCardMapper, mockContentCardSchemaData)
    }

    private fun runTest(runnable: Runnable) {
        mockStatic(ServiceProvider::class.java)
            .use { mocked ->
                mocked.`when`<Any> { ServiceProvider.getInstance() }
                    .thenReturn(mockServiceProvider)
                runnable.run()
            }
    }

    @Test
    fun `Small Image Template event handler receives a display event`() {
        runTest {
            val callback = mock(ContentCardUIEventListening::class.java)
            val handler = SmallImageTemplateEventHandler(callback)
            val event = UIEvent.Display(mockAepUI)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            verify(callback, times(1)).onDisplay(mockAepUI)
            verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISPLAY)
        }
    }

    @Test
    fun `Small Image Template event handler receives a dismiss event`() {
        runTest {
            val callback = mock(ContentCardUIEventListening::class.java)
            val handler = SmallImageTemplateEventHandler(callback)
            val event = UIEvent.Dismiss(mockAepUI)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            verify(callback, times(1)).onDismiss(mockAepUI)
            verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISMISS)
        }
    }

    @Test
    fun `Small Image Template event handler receives a click event`() {
        runTest {
            val callback = mock(ContentCardUIEventListening::class.java)
            val handler = SmallImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockAepUI, action)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            verify(callback, times(1)).onInteract(mockAepUI, "button1", "http://example.com")
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Small Image Template event handler receives a click event but callback on interact returns true`() {
        runTest {
            val callback = mock(ContentCardUIEventListening::class.java)
            `when`(callback.onInteract(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(true)
            val handler = SmallImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockAepUI, action)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            verify(callback, times(1)).onInteract(mockAepUI, "button1", "http://example.com")
            verify(mockUriOpening, never()).openUri(anyString())
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Small Image Template event handler receives a click event with no action url`() {
        runTest {
            val callback = mock(ContentCardUIEventListening::class.java)
            val handler = SmallImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = null)
            val event = UIEvent.Interact(mockAepUI, action)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            verify(callback, times(1)).onInteract(mockAepUI, "button1", null)
            verify(mockUriOpening, never()).openUri(anyString())
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Small Image Template event handler receives a click event with no callback provided`() {
        runTest {
            val handler = SmallImageTemplateEventHandler(null)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockAepUI, action)
            val propositionId = "propId"

            handler.onEvent(event, propositionId)

            // verify that the track is still called and that the url is still opened
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }
}
