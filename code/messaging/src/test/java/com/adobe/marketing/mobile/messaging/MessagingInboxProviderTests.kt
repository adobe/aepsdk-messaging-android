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
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.contentCardMap
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import com.adobe.marketing.mobile.services.DeviceInforming
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.reset
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import io.mockk.verify as mockkVerify

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
    private lateinit var mockServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var contentCardSchemaData: ContentCardSchemaData
    private lateinit var propositionItem: PropositionItem
    private lateinit var proposition: Proposition
    private lateinit var inboxPropositionItem: PropositionItem
    private lateinit var inboxProposition: Proposition
    private lateinit var mockInboxTemplate: InboxTemplate

    @Mock
    private lateinit var serviceProvider: ServiceProvider

    @Mock
    private lateinit var deviceInfoService: DeviceInforming

    @Before
    fun setup() {
        // Mock ServiceProvider and DeviceInfoService for Surface creation
        mockServiceProvider = mockStatic(ServiceProvider::class.java)
        serviceProvider = mock(ServiceProvider::class.java)
        deviceInfoService = mock(DeviceInforming::class.java)

        mockServiceProvider.`when`<ServiceProvider> { ServiceProvider.getInstance() }
            .thenReturn(serviceProvider)
        whenever(serviceProvider.deviceInfoService).thenReturn(deviceInfoService)
        whenever(deviceInfoService.applicationPackageName).thenReturn("com.adobe.marketing.mobile.messagingsample")

        surface = Surface("card/ms")
        messagingInboxProvider = MessagingInboxProvider(surface)
        mockMessaging = mockStatic(Messaging::class.java)

        contentCardSchemaData = mock(ContentCardSchemaData::class.java)
        propositionItem = mock(PropositionItem::class.java)
        proposition = mock(Proposition::class.java)

        whenever(propositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(contentCardSchemaData.content).thenReturn(contentCardMap)
        whenever(contentCardSchemaData.meta).thenReturn(metaMap)
        whenever(propositionItem.proposition).thenReturn(proposition)
        whenever(proposition.activityId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        whenever(propositionItem.contentCardSchemaData).thenReturn(contentCardSchemaData)
        whenever(proposition.items).thenReturn(listOf(propositionItem))

        inboxPropositionItem = mock(PropositionItem::class.java)
        inboxProposition = mock(Proposition::class.java)
        whenever(inboxPropositionItem.schema).thenReturn(SchemaType.INBOX)
        whenever(inboxProposition.items).thenReturn(listOf(inboxPropositionItem))
        mockInboxTemplate = InboxTemplate(
            id = "test-inbox-template",
            heading = AepText("Test Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages"),
            isUnreadEnabled = true
        )

        // Mock ContentCardSchemaDataUtils for read status and inbox template creation
        mockkObject(ContentCardSchemaDataUtils)
        every { ContentCardSchemaDataUtils.getReadStatus(any()) } returns null
        every { ContentCardSchemaDataUtils.buildTemplate(any()) } answers { callOriginal() }
        every { ContentCardSchemaDataUtils.getAepUI(any(), any()) } answers { callOriginal() }
        every { ContentCardSchemaDataUtils.createInboxTemplate(any()) } returns mockInboxTemplate
    }

    @After
    fun tearDown() {
        mockMessaging.close()
        mockServiceProvider.close()
        reset(serviceProvider)
        reset(deviceInfoService)
        clearAllMocks()
        ContentCardMapper.instance.clear()
    }

    @Test
    fun `getInboxUI emits Loading state first`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
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
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertNotNull("Template should not be null", successState.template)
        assertEquals(1, successState.items.size)
        assertEquals("testId", successState.items.first().getTemplate().id)
    }

    @Test
    fun `getInboxUI returns Success with empty items when no content card propositions found`() =
        runTest {
            mockMessaging.`when`<Unit> {
                Messaging.getPropositionsForSurfaces(any(), any())
            }.thenAnswer { invocation ->
                val callback =
                    invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
                callback.call(mapOf(surface to listOf(inboxProposition)))
            }

            val flow = messagingInboxProvider.getInboxUI()
            val states = flow.take(2).toList()

            assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
            val successState = states[1] as InboxUIState.Success
            assertTrue("Items should be empty", successState.items.isEmpty())
        }

    @Test
    fun `getInboxUI handles proposition with non-content-card schema`() = runTest {
        whenever(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue(
            "Items should be empty when schema is not CONTENT_CARD",
            successState.items.isEmpty()
        )
    }

    @Test
    fun `getInboxUI handles null contentCardSchemaData`() = runTest {
        whenever(propositionItem.contentCardSchemaData).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI returns Success with empty items when buildTemplate returns null for content card`() = runTest {
        // Mock buildTemplate to return null, simulating invalid content card data
        every { ContentCardSchemaDataUtils.buildTemplate(any()) } returns null

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        // Should still succeed but with empty items since buildTemplate returned null
        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty when buildTemplate returns null", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI returns Success with empty items when getAepUI returns null`() = runTest {
        // Mock getAepUI to return null, simulating unsupported template type
        every { ContentCardSchemaDataUtils.getAepUI(any(), any()) } returns null

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        // Should still succeed but with empty items since getAepUI returned null
        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertTrue("Items should be empty when getAepUI returns null", successState.items.isEmpty())
    }

    @Test
    fun `getInboxUI filters out propositions with non content card item`() = runTest {
        // Create a proposition where item is not content card
        val mixedProposition = mock(Proposition::class.java)
        val nonContentCardItem = mock(PropositionItem::class.java)
        whenever(nonContentCardItem.schema).thenReturn(SchemaType.HTML_CONTENT)
        whenever(mixedProposition.items).thenReturn(listOf(nonContentCardItem))

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            // Return both valid content card and non-content card proposition
            callback.call(mapOf(surface to listOf(inboxProposition, proposition, mixedProposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        // Should only have 1 item from the valid content card proposition
        assertEquals("Should only include valid content cards", 1, successState.items.size)
    }

    @Test
    fun `getInboxUI handles mixed valid and invalid content cards`() = runTest {
        // Create a second proposition that will fail buildTemplate
        val invalidProposition = mock(Proposition::class.java)
        val invalidPropositionItem = mock(PropositionItem::class.java)
        val invalidContentCardSchemaData = mock(ContentCardSchemaData::class.java)

        whenever(invalidProposition.activityId).thenReturn("invalidId")
        whenever(invalidProposition.items).thenReturn(listOf(invalidPropositionItem))
        whenever(invalidPropositionItem.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(invalidPropositionItem.proposition).thenReturn(invalidProposition)
        whenever(invalidPropositionItem.contentCardSchemaData).thenReturn(invalidContentCardSchemaData)
        whenever(invalidContentCardSchemaData.content).thenReturn(emptyMap<String, Any>()) // Invalid content
        whenever(invalidContentCardSchemaData.meta).thenReturn(emptyMap())
        invalidContentCardSchemaData.parent = invalidPropositionItem

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition, invalidProposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        // Should only have 1 valid item, invalid one should be filtered out
        assertEquals("Should only include valid content cards", 1, successState.items.size)
        assertEquals("Valid card should have testId", "testId", successState.items.first().getTemplate().id)
    }

    @Test
    fun `getInboxUI returns Error when null proposition map returned`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue(
            "Error message should contain error info",
            errorState.error.message?.contains("Received null propositions map for surface") == true
        )
    }

    @Test
    fun `getInboxUI return Error when empty proposition map returned`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(emptyMap())
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue(
            "Error message should contain error info",
            errorState.error.message?.contains("Received null propositions map for surface") == true
        )
    }

    @Test
    fun `getInboxUI returns Error when createInboxTemplate returns null`() = runTest {
        // Mock createInboxTemplate to return null
        every { ContentCardSchemaDataUtils.createInboxTemplate(any()) } returns null

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue(
            "Error message should indicate template creation failure",
            errorState.error.message?.contains("Failed to create inbox template") == true
        )
    }

    @Test
    fun `getInboxUI handles error with null errorName`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            // Pass null to simulate error with no name
            callback.fail(null)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue(
            "Error message should contain 'Unknown error'",
            errorState.error.message?.contains("Unknown error") == true
        )
    }

    @Test
    fun `getInboxUI returns error when result fails with exception`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
        val errorState = states[1] as InboxUIState.Error
        assertTrue(
            "Error message should indicate proposition fetch failure",
            errorState.error.message?.contains("Failed to get propositions") == true
        )
    }

    @Test
    fun `multiple calls to getInboxUI return consistent flow`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow1 = messagingInboxProvider.getInboxUI()
        val flow2 = messagingInboxProvider.getInboxUI()

        val state1 = flow1.take(2).toList()
        val state2 = flow2.take(2).toList()

        assertTrue("First flow should emit Loading", state1[0] is InboxUIState.Loading)
        assertTrue("Second flow should emit Loading", state2[0] is InboxUIState.Loading)
    }

    // Read Status Tests
    @Test
    fun `getInboxUI returns unread status when getReadStatus returns false`() = runTest {
        every { ContentCardSchemaDataUtils.getReadStatus("testId") } returns false

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertEquals(1, successState.items.size)

        val cardState = successState.items.first().getState()
        assertNotNull("Card state should not be null", cardState)
        assertFalse("Card should be unread when getReadStatus returns false", cardState.read == true)
        mockkVerify(exactly = 1) { ContentCardSchemaDataUtils.getReadStatus("testId") }
    }

    @Test
    fun `getInboxUI returns read status when getReadStatus returns true`() = runTest {
        every { ContentCardSchemaDataUtils.getReadStatus("testId") } returns true

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertEquals(1, successState.items.size)

        val cardState = successState.items.first().getState()
        assertNotNull("Card state should not be null", cardState)
        assertTrue("Card should be read when getReadStatus returns true", cardState.read == true)
        mockkVerify(exactly = 1) { ContentCardSchemaDataUtils.getReadStatus("testId") }
    }

    @Test
    fun `getInboxUI returns null read status when getReadStatus returns null`() = runTest {
        every { ContentCardSchemaDataUtils.getReadStatus("testId") } returns null

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertEquals(1, successState.items.size)

        val cardState = successState.items.first().getState()
        assertNotNull("Card state should not be null", cardState)
        assertNull("Card read status should be null when getReadStatus returns null", cardState.read)
        mockkVerify(exactly = 1) { ContentCardSchemaDataUtils.getReadStatus("testId") }
    }

    @Test
    fun `getInboxUI correctly handles read status for multiple content cards`() = runTest {
        // Create a second proposition with different read status
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        whenever(proposition2.activityId).thenReturn("testId2")
        whenever(proposition2.items).thenReturn(listOf(propositionItem2))
        whenever(propositionItem2.schema).thenReturn(SchemaType.CONTENT_CARD)
        whenever(propositionItem2.proposition).thenReturn(proposition2)
        whenever(propositionItem2.contentCardSchemaData).thenReturn(contentCardSchemaData2)
        whenever(contentCardSchemaData2.content).thenReturn(contentCardMap)
        whenever(contentCardSchemaData2.meta).thenReturn(metaMap)
        contentCardSchemaData2.parent = propositionItem2

        // First card is read, second card is unread
        every { ContentCardSchemaDataUtils.getReadStatus("testId") } returns true
        every { ContentCardSchemaDataUtils.getReadStatus("testId2") } returns false

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition, proposition2)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertEquals(2, successState.items.size)

        // Verify both cards have their respective read statuses
        val firstCard = successState.items.find { it.getTemplate().id == "testId" }
        val secondCard = successState.items.find { it.getTemplate().id == "testId2" }

        assertNotNull("First card should be present", firstCard)
        assertNotNull("Second card should be present", secondCard)
        assertTrue("First card should be read", firstCard?.getState()?.read == true)
        assertFalse("Second card should be unread", secondCard?.getState()?.read == true)
    }

    @Test
    fun `getInboxUI does not call getReadStatus when isUnreadEnabled is false`() = runTest {
        every { ContentCardSchemaDataUtils.createInboxTemplate(any()) } returns InboxTemplate(
            id = "test-inbox-template",
            heading = AepText("Test Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages"),
            isUnreadEnabled = false
        )

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success

        mockkVerify(exactly = 0) { ContentCardSchemaDataUtils.getReadStatus(any()) }

        val cardState = successState.items.first().getState()
        assertNull("Card read status should be null when isUnreadEnabled is false", cardState.read)
    }

    @Test
    fun `refresh emits Loading then Error on API failure`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("First state should be Loading", states[0] is InboxUIState.Loading)
        assertTrue("Second state should be Error", states[1] is InboxUIState.Error)
    }

    @Test
    fun `refresh with different data updates state`() = runTest {
        var callCount = 0

        // Create a second proposition with different data
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        whenever(proposition2.activityId).thenReturn("differentTestId")
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
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            val dataToReturn = if (callCount == 1) {
                mapOf(surface to listOf(inboxProposition, proposition))
            } else {
                mapOf(surface to listOf(inboxProposition, proposition2))
            }
            callback.call(dataToReturn)
        }

        val flow = messagingInboxProvider.getInboxUI()
        val initialStates = flow.take(2).toList()

        assertTrue("Initial state should be Success", initialStates[1] is InboxUIState.Success)
        val initialSuccessState = initialStates[1] as InboxUIState.Success
        assertEquals("Initial items should have 1 item", 1, initialSuccessState.items.size)
        assertEquals("Initial item should have testId", "testId", initialSuccessState.items.first().getTemplate().id)

        // Trigger refresh and collect states after refresh
        messagingInboxProvider.refresh()
        val refreshedStates = flow.take(2).toList()

        assertTrue("Should have called API at least twice", callCount >= 2)
        assertTrue("Refreshed state should include Loading", refreshedStates[0] is InboxUIState.Loading)
        assertTrue("Refreshed state should include Success", refreshedStates[1] is InboxUIState.Success)
        val refreshedSuccessState = refreshedStates[1] as InboxUIState.Success
        assertEquals("Refreshed items should have 1 item", 1, refreshedSuccessState.items.size)
        assertEquals("Refreshed item should have differentTestId", "differentTestId", refreshedSuccessState.items.first().getTemplate().id)
    }

    @Test
    fun `refresh emits Loading then Success`() = runTest {
        var callCount = 0

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            callCount++
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("First state should be Loading", states[0] is InboxUIState.Loading)
        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        assertTrue("API should have been called at least once", callCount >= 1)
    }

    @Test
    fun `fetchInbox selects first inbox proposition in the list`() = runTest {
        val firstInboxProp = mock(Proposition::class.java)
        val secondInboxProp = mock(Proposition::class.java)
        val firstInboxItem = mock(PropositionItem::class.java)
        val secondInboxItem = mock(PropositionItem::class.java)

        whenever(firstInboxItem.schema).thenReturn(SchemaType.INBOX)
        whenever(secondInboxItem.schema).thenReturn(SchemaType.INBOX)
        whenever(firstInboxProp.items).thenReturn(listOf(firstInboxItem))
        whenever(secondInboxProp.items).thenReturn(listOf(secondInboxItem))

        val firstTemplate = InboxTemplate(
            id = "first-inbox",
            heading = AepText("First Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages")
        )
        val secondTemplate = InboxTemplate(
            id = "second-inbox",
            heading = AepText("Second Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = AepText("No messages")
        )
        every { ContentCardSchemaDataUtils.createInboxTemplate(firstInboxProp) } returns firstTemplate
        every { ContentCardSchemaDataUtils.createInboxTemplate(secondInboxProp) } returns secondTemplate

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(firstInboxProp, secondInboxProp)))
        }

        val states = messagingInboxProvider.getInboxUI().take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        assertEquals(
            "Should select the first inbox proposition in the server-ordered list",
            "first-inbox",
            (states[1] as InboxUIState.Success).template.id
        )
    }

    @Test
    fun `Inbox proposition item is stored in ContentCardMapper on success`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success

        val storedItem = ContentCardMapper.instance.getInboxPropositionItem(successState.template.id)
        assertNotNull("Inbox proposition item should be stored in mapper with inbox id", storedItem)
    }

    @Test
    fun `Success state has displayed flag set to false initially`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val flow = messagingInboxProvider.getInboxUI()
        val states = flow.take(2).toList()

        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)
        val successState = states[1] as InboxUIState.Success
        assertFalse("displayed flag should be false initially", successState.displayed)
    }

    @Test
    fun `onInboxEvent Display updates the inbox state flow`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        // Start collecting all states in the background
        val states = mutableListOf<InboxUIState>()
        val job = launch {
            messagingInboxProvider.getInboxUI().collect { state ->
                states.add(state)
            }
        }

        // Wait for initial load to complete (Loading + Success)
        advanceUntilIdle()

        // Verify we have at least 2 states (Loading and Success)
        assertTrue("Should have at least 2 states", states.size >= 2)
        assertTrue("First state should be Loading", states[0] is InboxUIState.Loading)
        assertTrue("Second state should be Success", states[1] is InboxUIState.Success)

        val initialSuccess = states[1] as InboxUIState.Success
        assertFalse("Initial displayed flag should be false", initialSuccess.displayed)

        // Update the state by notifying the provider of display event
        messagingInboxProvider.inboxEventObserver.onInboxEvent(InboxEvent.Display(initialSuccess))

        // Wait for the update to propagate
        advanceUntilIdle()

        // Verify the updated state was collected
        assertTrue("Should have at least 3 states", states.size >= 3)
        val finalState = states.last()
        assertTrue("Final state should be Success", finalState is InboxUIState.Success)
        assertTrue("displayed flag should be true after update", (finalState as InboxUIState.Success).displayed)

        job.cancel()
    }

    @Test
    fun `onEvent Dismiss removes dismissed item from inbox state`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback =
                invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(inboxProposition, proposition)))
        }

        val states = mutableListOf<InboxUIState>()
        val job = launch {
            messagingInboxProvider.getInboxUI().collect { state ->
                states.add(state)
            }
        }

        advanceUntilIdle()

        assertTrue("Should have at least 2 states", states.size >= 2)
        val initialSuccess = states[1] as InboxUIState.Success
        assertEquals("Should have 1 item initially", 1, initialSuccess.items.size)

        // Dismiss the only item
        val itemToDismiss = initialSuccess.items.first()
        messagingInboxProvider.inboxEventObserver.onEvent(UIEvent.Dismiss(itemToDismiss))

        advanceUntilIdle()

        assertTrue("Should have at least 3 states after dismiss", states.size >= 3)
        val finalState = states.last() as InboxUIState.Success
        assertTrue("Items should be empty after dismiss", finalState.items.isEmpty())

        job.cancel()
    }
}
