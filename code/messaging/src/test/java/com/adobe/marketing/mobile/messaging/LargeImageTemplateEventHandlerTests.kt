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

import com.adobe.marketing.mobile.MessagingEdgeEventType
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.uri.UriOpening
import io.mockk.clearAllMocks
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
import io.mockk.verify as mockkVerify

class LargeImageTemplateEventHandlerTests {

    @Mock
    private lateinit var mockLargeImageUI: LargeImageUI
    @Mock
    private lateinit var mockLargeImageCardUIState: LargeImageCardUIState
    @Mock
    private lateinit var mockLargeImageTemplate: LargeImageTemplate
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
        mockLargeImageUI = mock(LargeImageUI::class.java)
        mockLargeImageCardUIState = mock(LargeImageCardUIState::class.java)
        mockLargeImageTemplate = mock(LargeImageTemplate::class.java)
        mockServiceProvider = mock(ServiceProvider::class.java)
        mockUriOpening = mock(UriOpening::class.java)
        mockContentCardMapper = mock(ContentCardMapper::class.java)
        mockContentCardSchemaData = mock(ContentCardSchemaData::class.java)

        `when`(mockContentCardMapper.getContentCardSchemaData(anyString())).thenReturn(mockContentCardSchemaData)
        `when`(mockServiceProvider.uriService).thenReturn(mockUriOpening)
        `when`(mockLargeImageTemplate.id).thenReturn("mockId")
        `when`(mockLargeImageTemplate.getType()).thenReturn(AepUITemplateType.LARGE_IMAGE)
        `when`(mockLargeImageUI.getTemplate()).thenReturn(mockLargeImageTemplate)
        `when`(mockLargeImageUI.getState()).thenReturn(mockLargeImageCardUIState)

        mockkObject(ContentCardMapper)
        every { ContentCardMapper.instance } returns mockContentCardMapper

        mockkObject(ContentCardSchemaDataUtils)
        every { ContentCardSchemaDataUtils.setReadStatus(any(), any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        reset(mockLargeImageUI, mockLargeImageTemplate, mockServiceProvider, mockUriOpening, mockContentCardMapper, mockContentCardSchemaData)
        clearAllMocks()
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
    fun `Large Image Template event handler receives a display event`() {
        runTest {
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val event = UIEvent.Display(mockLargeImageUI)

            handler.onEvent(event, "propositionId")

            verify(mockLargeImageCardUIState, times(1)).displayed
            verify(callback, times(1)).onDisplay(mockLargeImageUI)
            verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISPLAY)
        }
    }

    @Test
    fun `Large Image Template event handler receives a display event when card already displayed`() {
        runTest {
            `when`(mockLargeImageCardUIState.displayed).thenReturn(true)
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val event = UIEvent.Display(mockLargeImageUI)

            handler.onEvent(event, "propositionId")

            verify(mockLargeImageCardUIState, times(1)).displayed
            verify(callback, times(0)).onDisplay(mockLargeImageUI)
            verify(mockContentCardSchemaData, times(0)).track(null, MessagingEdgeEventType.DISPLAY)
        }
    }

    @Test
    fun `Large Image Template event handler receives a dismiss event`() {
        runTest {
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val event = UIEvent.Dismiss(mockLargeImageUI)

            handler.onEvent(event, "propositionId")

            verify(mockLargeImageCardUIState, times(1)).dismissed
            verify(callback, times(1)).onDismiss(mockLargeImageUI)
            verify(mockContentCardSchemaData, times(1)).track(null, MessagingEdgeEventType.DISMISS)
        }
    }

    @Test
    fun `Large Image Template event handler receives a dismiss event when card already dismissed`() {
        runTest {
            `when`(mockLargeImageCardUIState.dismissed).thenReturn(true)
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val event = UIEvent.Dismiss(mockLargeImageUI)

            handler.onEvent(event, "propositionId")

            verify(mockLargeImageCardUIState, times(1)).dismissed
            verify(callback, times(0)).onDismiss(mockLargeImageUI)
            verify(mockContentCardSchemaData, times(0)).track(null, MessagingEdgeEventType.DISMISS)
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event`() {
        runTest {
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            verify(callback, times(1)).onInteract(mockLargeImageUI, "button1", "http://example.com")
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event but callback on interact returns true`() {
        runTest {
            val callback = mock(ContentCardUIEventListener::class.java)
            `when`(callback.onInteract(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(true)
            val handler = LargeImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            verify(callback, times(1)).onInteract(mockLargeImageUI, "button1", "http://example.com")
            verify(mockUriOpening, never()).openUri(anyString())
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event with no action url`() {
        runTest {
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = null)
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            verify(callback, times(1)).onInteract(mockLargeImageUI, "button1", null)
            verify(mockUriOpening, never()).openUri(anyString())
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event with no callback provided`() {
        runTest {
            val handler = LargeImageTemplateEventHandler(null)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            // verify that the track is still called and that the url is still opened
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event and sets read status when read is not null`() {
        runTest {
            `when`(mockLargeImageCardUIState.read).thenReturn(false)
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            verify(callback, times(1)).onInteract(mockLargeImageUI, "button1", "http://example.com")
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
            mockkVerify(exactly = 1) { ContentCardSchemaDataUtils.setReadStatus("mockId", true) }
        }
    }

    @Test
    fun `Large Image Template event handler receives a click event and does not set read status when read is null`() {
        runTest {
            `when`(mockLargeImageCardUIState.read).thenReturn(null)
            val callback = mock(ContentCardUIEventListener::class.java)
            val handler = LargeImageTemplateEventHandler(callback)
            val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
            val event = UIEvent.Interact(mockLargeImageUI, action)

            handler.onEvent(event, "propositionId")

            verify(callback, times(1)).onInteract(mockLargeImageUI, "button1", "http://example.com")
            verify(mockUriOpening, times(1)).openUri("http://example.com")
            verify(mockContentCardSchemaData, times(1)).track("button1", MessagingEdgeEventType.INTERACT)
            mockkVerify(exactly = 0) { ContentCardSchemaDataUtils.setReadStatus(any(), any()) }
        }
    }
}
