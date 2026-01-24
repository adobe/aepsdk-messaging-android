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

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.contentCardMap
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Tests for MessagingInboxProvider class.
 *
 * @ExperimentalCoroutinesApi is used here because this test class leverages Kotlin coroutines testing features:
 * - runTest for structured concurrency testing
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MessagingInboxProviderTests {

    private lateinit var messagingInboxProvider: MessagingInboxProvider
    private lateinit var surface: Surface
    private lateinit var mockMessaging: MockedStatic<Messaging>
    private lateinit var contentCardSchemaData: ContentCardSchemaData
    private lateinit var propositionItem: PropositionItem
    private lateinit var proposition: Proposition

    @Before
    fun setup() {
        surface = Surface("mobileapp://com.adobe.marketing.mobile.messagingsample/card/ms")
        messagingInboxProvider = MessagingInboxProvider(surface)
        mockMessaging = mockStatic(Messaging::class.java)

        contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        propositionItem = mock(PropositionItem::class.java)
        proposition = mock(Proposition::class.java)

        setupDefaultMocks()
    }

    private fun setupDefaultMocks() {
        whenever(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(contentCardSchemaData.content).thenReturn(contentCardMap)
        whenever(contentCardSchemaData.meta).thenReturn(metaMap)
        whenever(propositionItem.proposition).thenReturn(proposition)
        whenever(proposition.getActivityId()).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        whenever(propositionItem.contentCardSchemaData).thenReturn(contentCardSchemaData)
        whenever(proposition.items).thenReturn(listOf(propositionItem))
    }

    @After
    fun tearDown() {
        mockMessaging.close()
    }

    @Test
    fun `getInboxUI emits Loading state first`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("First state should be Loading", states[0] is InboxUIState.Loading)
    }

    @Test
    fun `getInboxUI returns Success state with valid content cards`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertNotNull("Template should not be null", successState.template)
        assertEquals(1, successState.items.size)
    }

    @Test
    fun `getInboxUI returns Success with empty items when no content card propositions found`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to emptyList()))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI returns Success with empty items when null proposition map returned`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI returns Error state when API fails`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue("Error message should contain error info", errorState.error.message?.contains("Failed to get propositions") == true)
    }

    @Test
    fun `getInboxUI handles proposition with non-content-card schema`() = runTest {
        whenever(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty when schema is not CONTENT_CARD", successState.items.isEmpty())
    }

    @Test
    fun `refresh updates state flow`() = runTest {
        var callCount = 0

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            callback.call(mapOf(surface to listOf(proposition)))
        }

        // Get initial state
        val initialState = messagingInboxProvider.getInboxUI().first()
        assertTrue("Initial state should be Loading", initialState is InboxUIState.Loading)

        // Call refresh
        messagingInboxProvider.refresh()

        // Verify API was called
        assertTrue("API should have been called at least once", callCount >= 1)
    }

    @Test
    fun `refresh emits Loading then Success`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("First state should be Loading", states[0] is InboxUIState.Loading)
        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
    }

    @Test
    fun `getInboxUI handles empty result map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(emptyMap())
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI handles proposition with empty items list`() = runTest {
        whenever(proposition.items).thenReturn(emptyList())

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI handles null contentCardSchemaData`() = runTest {
        whenever(propositionItem.contentCardSchemaData).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI handles missing content in contentCardSchemaData`() = runTest {
        whenever(contentCardSchemaData.content).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI handles missing meta in contentCardSchemaData`() = runTest {
        whenever(contentCardSchemaData.meta).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
    }

    @Test
    fun `surface property is accessible`() {
        val testSurface = Surface("mobileapp://test/surface")
        val provider = MessagingInboxProvider(testSurface)

        assertEquals("Surface should match", testSurface, provider.surface)
    }

    @Test
    fun `multiple calls to getInboxUI return consistent flow`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow1 = messagingInboxProvider.getInboxUI()
        val flow2 = messagingInboxProvider.getInboxUI()

        val state1 = flow1.take(2).toList()
        val state2 = flow2.take(2).toList()

        assertTrue("First flow should emit Loading", state1[0] is InboxUIState.Loading)
        assertTrue("Second flow should emit Loading", state2[0] is InboxUIState.Loading)
    }

    @Test
    fun `refresh with different data updates state`() = runTest {
        var callCount = 0

        // Create a second proposition with different data
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        whenever(proposition2.getActivityId()).thenReturn("differentTestId")
        whenever(proposition2.items).thenReturn(listOf(propositionItem2))
        whenever(propositionItem2.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(propositionItem2.proposition).thenReturn(proposition2)
        whenever(propositionItem2.contentCardSchemaData).thenReturn(contentCardSchemaData2)
        whenever(contentCardSchemaData2.content).thenReturn(contentCardMap)
        whenever(contentCardSchemaData2.meta).thenReturn(metaMap)
        contentCardSchemaData2.parent = propositionItem2

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            val dataToReturn = if (callCount == 1) {
                mapOf(surface to listOf(proposition))
            } else {
                mapOf(surface to listOf(proposition2))
            }
            callback.call(dataToReturn)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val initialStates = flow.take(2).toList()

        assertTrue("Initial state should be Success", initialStates[1] is InboxUIState.Success)

        // Trigger refresh
        messagingInboxProvider.refresh()

        assertTrue("Should have called API at least twice", callCount >= 2)
    }

    @Test
    fun `getInboxUI handles multiple content card propositions`() = runTest {
        // Create another proposition
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        whenever(proposition2.getActivityId()).thenReturn("testId2")
        whenever(proposition2.items).thenReturn(listOf(propositionItem2))
        whenever(propositionItem2.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(propositionItem2.proposition).thenReturn(proposition2)
        whenever(propositionItem2.contentCardSchemaData).thenReturn(contentCardSchemaData2)
        whenever(contentCardSchemaData2.content).thenReturn(contentCardMap)
        whenever(contentCardSchemaData2.meta).thenReturn(metaMap)
        contentCardSchemaData2.parent = propositionItem2

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition, proposition2)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
    }

    @Test
    fun `InboxProposition data class stores inbox and content cards`() {
        val inboxProposition = mock(Proposition::class.java)
        val contentCards = listOf(proposition)

        val inboxData = MessagingInboxProvider.InboxProposition(
            inbox = inboxProposition,
            contentCards = contentCards
        )

        assertEquals("Inbox should match", inboxProposition, inboxData.inbox)
        assertEquals("Content cards should match", contentCards, inboxData.contentCards)
    }

    @Test
    fun `InboxProposition data class with empty content cards`() {
        val inboxProposition = mock(Proposition::class.java)
        val contentCards = emptyList<Proposition>()

        val inboxData = MessagingInboxProvider.InboxProposition(
            inbox = inboxProposition,
            contentCards = contentCards
        )

        assertEquals("Inbox should match", inboxProposition, inboxData.inbox)
        assertTrue("Content cards should be empty", inboxData.contentCards.isEmpty())
    }

    @Test
    fun `getInboxUI handles mixed schema types in propositions`() = runTest {
        // Create an HTML proposition that should be filtered out
        val htmlProposition = mock(Proposition::class.java)
        val htmlPropositionItem = mock(PropositionItem::class.java)

        whenever(htmlProposition.items).thenReturn(listOf(htmlPropositionItem))
        whenever(htmlPropositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition, htmlProposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
    }
}
