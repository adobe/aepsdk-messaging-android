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
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
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
import java.lang.ref.SoftReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class InboxEventObserverTests {

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
        ContentCardMapper.instance.clear()
    }

    private fun createSuccessState(inboxId: String): InboxUIState.Success {
        val template = InboxTemplate(
            id = inboxId,
            heading = AepText("Test Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages")
        )
        return InboxUIState.Success(template = template, items = emptyList())
    }

    @Test
    fun `onInboxEvent Display tracks inbox proposition item from mapper`() {
        val observer = InboxEventObserver()

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)

        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        observer.onInboxEvent(InboxEvent.Display(createSuccessState("testInboxId")))

        verify(mockPropositionItem, times(1)).track(MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `onInboxEvent Display does nothing when no inbox proposition item is stored for given id`() {
        val observer = InboxEventObserver()

        // No exception should be thrown when mapper has no inbox proposition item for this ID
        observer.onInboxEvent(InboxEvent.Display(createSuccessState("nonExistentInboxId")))
    }

    @Test
    fun `onInboxEvent Display tracks correct inbox proposition for multiple inboxes`() {
        val observer = InboxEventObserver()

        val mockPropositionItem1 = mock(PropositionItem::class.java)
        val mockProposition1 = mock(Proposition::class.java)
        mockPropositionItem1.propositionReference = SoftReference(mockProposition1)

        val mockPropositionItem2 = mock(PropositionItem::class.java)
        val mockProposition2 = mock(Proposition::class.java)
        mockPropositionItem2.propositionReference = SoftReference(mockProposition2)

        ContentCardMapper.instance.storeInboxPropositionItem("inbox1", mockPropositionItem1)
        ContentCardMapper.instance.storeInboxPropositionItem("inbox2", mockPropositionItem2)

        observer.onInboxEvent(InboxEvent.Display(createSuccessState("inbox2")))

        verify(mockPropositionItem1, times(0)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockPropositionItem2, times(1)).track(MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `onEvent delegates content card display event to ContentCardEventObserver`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = InboxEventObserver(ContentCardEventObserver(callback))
        val event = UIEvent.Display(mockAepUI)

        observer.onEvent(event)

        val expectedEvent = UIEvent.Display(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>)
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }

    @Test
    fun `onEvent delegates content card dismiss event to ContentCardEventObserver`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = InboxEventObserver(ContentCardEventObserver(callback))
        val event = UIEvent.Dismiss(mockAepUI)

        observer.onEvent(event)

        val expectedEvent = UIEvent.Dismiss(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>)
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }

    @Test
    fun `onEvent delegates content card interact event to ContentCardEventObserver`() {
        val callback = mock(ContentCardUIEventListener::class.java)
        val observer = InboxEventObserver(ContentCardEventObserver(callback))
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        val event = UIEvent.Interact(mockAepUI, action)

        observer.onEvent(event)

        val expectedEvent = UIEvent.Interact(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>, UIAction.Click(id = "button1", actionUrl = "http://example.com"))
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }

    @Test
    fun `InboxEventObserver without observers still handles inbox events`() {
        val observer = InboxEventObserver()

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)

        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        // onInboxEvent should work even without item observers
        observer.onInboxEvent(InboxEvent.Display(createSuccessState("testInboxId")))

        // Verify inbox display tracking still happens
        verify(mockPropositionItem, times(1)).track(MessagingEdgeEventType.DISPLAY)

        // onEvent with no item observers should delegate to default ContentCardEventObserver
        val itemEvent = UIEvent.Display(mockAepUI)
        observer.onEvent(itemEvent)

        // Verify the default ContentCardEventObserver was invoked
        val expectedEvent = UIEvent.Display(mockAepUI as AepUI<SmallImageTemplate, SmallImageCardUIState>)
        verify(mockSmallImageTemplateEventHandler.constructed()[0], times(1)).onEvent(expectedEvent, "mockId")
    }
}
