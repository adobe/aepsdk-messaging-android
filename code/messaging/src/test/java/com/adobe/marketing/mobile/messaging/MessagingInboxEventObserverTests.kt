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
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.lang.ref.SoftReference

/**
 * Tests for [MessagingInboxEventObserver].
 * Verifies that inbox events trigger tracking and provider state update, and item events are forwarded.
 */
class MessagingInboxEventObserverTests {

    @After
    fun tearDown() {
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
    fun `onInboxEvent Display performs tracking and calls provider onInboxEvent`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val observer = MessagingInboxEventObserver(mockProvider, null)

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)
        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        val successState = createSuccessState("testInboxId")
        val event = InboxEvent.Display(successState)
        observer.onInboxEvent(event)

        verify(mockPropositionItem, times(1)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockProvider, times(1)).onInboxEvent(event)
    }

    @Test
    fun `onInboxEvent Display calls provider onInboxEvent even when no proposition item in mapper`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val observer = MessagingInboxEventObserver(mockProvider, null)

        val successState = createSuccessState("noMappingId")
        val event = InboxEvent.Display(successState)
        observer.onInboxEvent(event)

        verify(mockProvider, times(1)).onInboxEvent(event)
    }

    @Test
    fun `onEvent forwards to provided itemEventObserver`() {
        val mockProvider = mock(MessagingInboxProvider::class.java)
        val mockItemObserver = mock(AepUIEventObserver::class.java)
        val observer = MessagingInboxEventObserver(mockProvider, mockItemObserver)

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
        val observer = MessagingInboxEventObserver(mockProvider, null)

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
