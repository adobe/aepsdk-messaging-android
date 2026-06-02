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
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepInboxEventObserver
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.lang.ref.SoftReference

/**
 * Tests for [InboxEventObserver].
 * Verifies that inbox events trigger tracking and provider state update, and item events are forwarded.
 */
class InboxEventObserverTests {

    @After
    fun tearDown() {
        ContentCardMapper.instance.clear()
    }

    private fun createSuccessState(inboxId: String, displayed: Boolean = false): InboxUIState.Success {
        val template = InboxTemplate(
            id = inboxId,
            heading = AepText("Test Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages")
        )
        return InboxUIState.Success(template = template, items = emptyList(), displayed = displayed)
    }

    @Test
    fun `onInboxEvent Display performs tracking and calls provider onInboxEvent`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxEventObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxEventObserver)
        val observer = InboxEventObserver(mockProvider, null)

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)
        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        val successState = createSuccessState("testInboxId")
        val event = InboxEvent.Display(successState)
        observer.onInboxEvent(event)

        verify(mockPropositionItem, times(1)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockInboxEventObserver, times(1)).onInboxEvent(event)
    }

    @Test
    fun `onInboxEvent Display does not track but calls provider onInboxEvent even when no proposition item in mapper`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxEventObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxEventObserver)
        val observer = InboxEventObserver(mockProvider, null)

        val successState = createSuccessState("noMappingId")
        val event = InboxEvent.Display(successState)
        observer.onInboxEvent(event)

        verify(mockInboxEventObserver, times(1)).onInboxEvent(event)
    }

    @Test
    fun `onInboxEvent Display does not track but calls provider onInboxEvent even  when no inbox proposition item is stored for given id`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mock(AepInboxEventObserver::class.java))
        val observer = InboxEventObserver(mockProvider, null)

        val mockPropositionItem = mock(PropositionItem::class.java)
        ContentCardMapper.instance.storeInboxPropositionItem("otherId", mockPropositionItem)

        val successState = createSuccessState("nonExistentInboxId")
        // No exception should be thrown when mapper has no inbox proposition item for this ID
        observer.onInboxEvent(InboxEvent.Display(successState))

        verify(mockPropositionItem, never()).track(MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `onInboxEvent Display tracks correct inbox proposition for multiple inboxes`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mock(AepInboxEventObserver::class.java))
        val observer = InboxEventObserver(mockProvider, null)

        val mockPropositionItem1 = mock(PropositionItem::class.java)
        val mockProposition1 = mock(Proposition::class.java)
        mockPropositionItem1.propositionReference = SoftReference(mockProposition1)

        val mockPropositionItem2 = mock(PropositionItem::class.java)
        val mockProposition2 = mock(Proposition::class.java)
        mockPropositionItem2.propositionReference = SoftReference(mockProposition2)

        ContentCardMapper.instance.storeInboxPropositionItem("inbox1", mockPropositionItem1)
        ContentCardMapper.instance.storeInboxPropositionItem("inbox2", mockPropositionItem2)

        val successState = createSuccessState("inbox2")
        observer.onInboxEvent(InboxEvent.Display(successState))

        verify(mockPropositionItem1, times(0)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockPropositionItem2, times(1)).track(MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `onInboxEvent Display skips tracking but still calls provider when displayed is true`() {
        // Covers the case where InboxEventObserver is called with displayed=true directly,
        // e.g. configuration change re-entry where the provider's surviving state has displayed=true.
        // The provider is always notified; only tracking is guarded by the displayed flag.
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxEventObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxEventObserver)
        val observer = InboxEventObserver(mockProvider, null)

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)
        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        val event = InboxEvent.Display(createSuccessState("testInboxId", displayed = true))
        observer.onInboxEvent(event)

        verify(mockPropositionItem, never()).track(MessagingEdgeEventType.DISPLAY)
        verify(mockInboxEventObserver, times(1)).onInboxEvent(event)
    }

    @Test
    fun `onInboxEvent Display tracks only on displayed=false but always calls provider`() {
        // Simulates the full lifecycle:
        // 1. displayed=false → tracking fires, provider called
        // 2. displayed=true  → tracking skipped, provider still called
        // 3. displayed=false → tracking fires again, provider called
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxEventObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxEventObserver)
        val observer = InboxEventObserver(mockProvider, null)

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)
        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        val eventNotDisplayed = InboxEvent.Display(createSuccessState("testInboxId", displayed = false))
        val eventDisplayed = InboxEvent.Display(createSuccessState("testInboxId", displayed = true))

        observer.onInboxEvent(eventNotDisplayed) // step 1: tracks + provider called
        observer.onInboxEvent(eventDisplayed) // step 2: no tracking, provider still called
        observer.onInboxEvent(eventNotDisplayed) // step 3: tracks again + provider called

        verify(mockPropositionItem, times(2)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockInboxEventObserver, times(2)).onInboxEvent(eventNotDisplayed)
        verify(mockInboxEventObserver, times(1)).onInboxEvent(eventDisplayed)
    }

    @Test
    fun `onEvent forwards to provided itemEventObserver`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxObserver)
        val mockItemObserver = mock(AepUIEventObserver::class.java)
        val observer = InboxEventObserver(mockProvider, mockItemObserver)

        val mockAepUI = mock(AepUI::class.java)
        val mockTemplate = mock(SmallImageTemplate::class.java)
        `when`(mockTemplate.id).thenReturn("card1")
        `when`(mockTemplate.getType()).thenReturn(AepUITemplateType.SMALL_IMAGE)
        `when`(mockAepUI.getTemplate()).thenReturn(mockTemplate)

        val event = UIEvent.Display(mockAepUI)
        observer.onEvent(event)

        verify(mockItemObserver, times(1)).onEvent(event)
    }

    @Test
    fun `onEvent with null itemEventObserver uses default ContentCardEventObserver`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockInboxObserver = mock(AepInboxEventObserver::class.java)
        `when`(mockProvider.inboxEventObserver).thenReturn(mockInboxObserver)
        val observer = InboxEventObserver(mockProvider, null)

        val mockAepUI = mock(AepUI::class.java)
        val mockTemplate = mock(SmallImageTemplate::class.java)
        `when`(mockTemplate.id).thenReturn("card1")
        `when`(mockTemplate.getType()).thenReturn(AepUITemplateType.SMALL_IMAGE)
        `when`(mockAepUI.getTemplate()).thenReturn(mockTemplate)
        `when`(mockAepUI.getState()).thenReturn(SmallImageCardUIState())

        val event = UIEvent.Display(mockAepUI)
        // Should not throw; default ContentCardEventObserver handles the event
        observer.onEvent(event)
    }
}
