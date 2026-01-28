/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.state

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class InboxUIStateTests {

    private fun createTestInboxTemplate(): InboxTemplate {
        return InboxTemplate(
            heading = AepText("Test Heading"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )
    }

    // Success State Tests

    @Test
    fun `Success state with template and empty items`() {
        val template = createTestInboxTemplate()
        val state = InboxUIState.Success(template = template, items = emptyList())

        assertEquals(template, state.template)
        assertEquals(0, state.items.size)
    }

    @Test
    fun `Success state with template and items`() {
        val template = createTestInboxTemplate()
        val mockItem1 = mock(AepUI::class.java)
        val mockItem2 = mock(AepUI::class.java)
        val items = listOf(mockItem1, mockItem2)

        val state = InboxUIState.Success(template = template, items = items)

        assertEquals(template, state.template)
        assertEquals(2, state.items.size)
        assertEquals(mockItem1, state.items[0])
        assertEquals(mockItem2, state.items[1])
    }

    @Test
    fun `Success state item read value can be updated`() {
        val template = createTestInboxTemplate()
        val itemTemplate = SmallImageTemplate(id = "item1", title = AepText("Item Title"))
        val item = SmallImageUI(itemTemplate, SmallImageCardUIState(read = null))
        val state = InboxUIState.Success(template = template, items = listOf(item))

        // Verify initial read state is null
        assertNull(state.items[0].getState().read)

        // Update the item's read state
        val currentItemState = state.items[0].getState() as SmallImageCardUIState
        (state.items[0] as SmallImageUI).updateState(currentItemState.copy(read = true))

        // Verify the read state is now true
        assertEquals(true, state.items[0].getState().read)
    }

    // Error State Tests

    @Test
    fun `Error state with throwable`() {
        val exception = RuntimeException("Test error")
        val state = InboxUIState.Error(error = exception)

        assertEquals(exception, state.error)
        assertEquals("Test error", state.error.message)
    }

    // Sealed Interface Tests

    @Test
    fun `when expression covers all InboxUIState types`() {
        val states = listOf(
            InboxUIState.Loading,
            InboxUIState.Success(template = createTestInboxTemplate(), items = emptyList()),
            InboxUIState.Error(error = RuntimeException("Test"))
        )

        states.forEach { state ->
            val result = when (state) {
                is InboxUIState.Loading -> "loading"
                is InboxUIState.Success -> "success"
                is InboxUIState.Error -> "error"
            }

            when (state) {
                is InboxUIState.Loading -> assertEquals("loading", result)
                is InboxUIState.Success -> assertEquals("success", result)
                is InboxUIState.Error -> assertEquals("error", result)
            }
        }
    }
}
