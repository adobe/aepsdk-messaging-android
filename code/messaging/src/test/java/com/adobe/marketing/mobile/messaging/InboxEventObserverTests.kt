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
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.lang.ref.SoftReference

/**
 * Tests for [InboxEventObserver] (internal tracking-only observer).
 * Verifies that inbox display events result in correct tracking via ContentCardMapper.
 */
class InboxEventObserverTests {

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
    fun `onInboxEvent Display tracks inbox proposition item from mapper`() {
        val observer = InboxEventObserver()

        val mockPropositionItem = mock(PropositionItem::class.java)
        val mockProposition = mock(Proposition::class.java)
        mockPropositionItem.propositionReference = SoftReference(mockProposition)

        ContentCardMapper.instance.storeInboxPropositionItem("testInboxId", mockPropositionItem)

        val successState = createSuccessState("testInboxId")
        observer.onInboxEvent(InboxEvent.Display(successState))

        verify(mockPropositionItem, times(1)).track(MessagingEdgeEventType.DISPLAY)
    }

    @Test
    fun `onInboxEvent Display does nothing when no inbox proposition item is stored for given id`() {
        val observer = InboxEventObserver()

        val successState = createSuccessState("nonExistentInboxId")
        // No exception should be thrown when mapper has no inbox proposition item for this ID
        observer.onInboxEvent(InboxEvent.Display(successState))
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

        val successState = createSuccessState("inbox2")
        observer.onInboxEvent(InboxEvent.Display(successState))

        verify(mockPropositionItem1, times(0)).track(MessagingEdgeEventType.DISPLAY)
        verify(mockPropositionItem2, times(1)).track(MessagingEdgeEventType.DISPLAY)
    }
}
