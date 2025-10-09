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

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.contentCardMap
import com.adobe.marketing.mobile.messaging.ContentCardJsonDataUtils.metaMap
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.Test

/**
 * Tests for ContentCardUIProvider class.
 *
 * @ExperimentalCoroutinesApi is used here because this test class leverages Kotlin coroutines testing feature:
 * - runTest for structured concurrency testing
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ContentCardUIProviderTests {

    private lateinit var contentCardUIProvider: ContentCardUIProvider
    private lateinit var surface: Surface
    private lateinit var mockMessaging: MockedStatic<Messaging>
    private lateinit var contentCardSchemaData: ContentCardSchemaData
    private lateinit var propositionItem: PropositionItem
    private lateinit var proposition: Proposition

    @Before
    fun setup() {
        surface = Surface("mobileapp://com.adobe.marketing.mobile.messagingsample/card/ms")
        contentCardUIProvider = ContentCardUIProvider(surface)
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
        whenever(proposition.uniqueId).thenReturn("testId")
        contentCardSchemaData.parent = propositionItem
        whenever(propositionItem.contentCardSchemaData).thenReturn(contentCardSchemaData)
        whenever(proposition.items).thenReturn(listOf(propositionItem))
    }

    @Test
    fun `getContentCardUI returns success with valid template`() = runTest {

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles null proposition map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getContentCardUI handles empty proposition list`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to emptyList()))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles invalid schema type`() = runTest {
        whenever(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles missing content`() = runTest {
        whenever(contentCardSchemaData.content).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles API failure`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshContent triggers new content fetch`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        contentCardUIProvider.refreshContent()
        val flow = contentCardUIProvider.getContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getContent handles missing meta data`() = runTest {
        whenever(contentCardSchemaData.meta).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles proposition with no items`() = runTest {
        whenever(proposition.items).thenReturn(emptyList())

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUI handles null contentCardSchemaData`() = runTest {
        whenever(propositionItem.contentCardSchemaData).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @After
    fun tearDown() {
        mockMessaging.close()
    }

    @Test
    fun `getContent handles null result map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }
        val flow = contentCardUIProvider.getContent()
        val result = flow.first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getContent handles empty result map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(emptyMap())
        }

        val flow = contentCardUIProvider.getContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getContentCardUIFlow returns continuous flow with valid template`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()

        // Should emit initial value successfully
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles null proposition map in continuous flow`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getContentCardUIFlow handles empty proposition list`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to emptyList()))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles invalid schema type`() = runTest {
        whenever(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles missing content`() = runTest {
        whenever(contentCardSchemaData.content).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles API failure`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getContentCardUIFlow handles missing meta data`() = runTest {
        whenever(contentCardSchemaData.meta).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles proposition with no items`() = runTest {
        whenever(proposition.items).thenReturn(emptyList())

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles null contentCardSchemaData`() = runTest {
        whenever(propositionItem.contentCardSchemaData).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow handles empty result map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(emptyMap())
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow emits when Messaging getPropositionsForSurfaces returns different data`() = runTest {
        var callCount = 0

        // Create a second proposition with different uniqueId to ensure data actually changes
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        // Setup second proposition with different data
        whenever(proposition2.uniqueId).thenReturn("differentTestId")
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
            // Return different data on second call
            val dataToReturn = if (callCount == 1) {
                mapOf(surface to listOf(proposition))
            } else {
                mapOf(surface to listOf(proposition2)) // Different proposition
            }
            callback.call(dataToReturn)
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()

        // Collect exactly 2 emissions using async
        val emissionsDeferred = async {
            flow.take(2).toList()
        }

        // Process initial emission
        advanceUntilIdle()

        // Trigger refresh with different data
        contentCardUIProvider.refreshContent()
        advanceUntilIdle()

        // Get both emissions
        val emissions = emissionsDeferred.await()

        // Verify that we got exactly 2 emissions
        assertEquals("Should have exactly 2 emissions", 2, emissions.size)
        assertTrue("First emission should be successful", emissions[0].isSuccess)
        assertTrue("Second emission should be successful", emissions[1].isSuccess)
        assertEquals("Should have called API twice", 2, callCount)
    }

    @Test
    fun `getContentCardUIFlow automatically refreshes content on first collection`() = runTest {
        var apiCallCount = 0
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            apiCallCount++
            callback.call(mapOf(surface to listOf(proposition)))
        }

        // The flow should automatically trigger content refresh when collected
        val flow = contentCardUIProvider.getContentCardUIFlow()
        val result = flow.first()

        // Verify that the API was called automatically and content is emitted
        assertTrue("API should be called automatically", apiCallCount >= 1)
        assertTrue("Result should be successful", result.isSuccess)
        assertNotNull("Result should contain content", result.getOrNull())
        assertTrue("Result should not be empty", result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getContentCardUIFlow does not emit when refreshContent returns same data`() = runTest {
        var callCount = 0
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            // Always return the same data
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()

        // Start collecting to monitor emissions (this will trigger onStart)
        val emissions = mutableListOf<Result<List<com.adobe.marketing.mobile.aepcomposeui.AepUI<*, *>>>>()
        val collectJob = launch {
            flow.collect { result ->
                emissions.add(result)
            }
        }

        // Process initial emission from onStart
        advanceUntilIdle()
        val initialEmissionCount = emissions.size
        assertTrue("Should have initial emission", initialEmissionCount >= 1)
        assertTrue("Initial emission should be successful", emissions[0].isSuccess)

        // Trigger refresh with same data - should NOT cause new emission
        contentCardUIProvider.refreshContent()
        advanceUntilIdle()

        // Verify that refreshContent with same data did NOT trigger a new emission
        assertEquals("Should NOT have new emission when data is unchanged", initialEmissionCount, emissions.size)

        // Verify that refreshContent actually called the API twice (onStart + manual refresh)
        assertEquals("Should have called API twice", 2, callCount)

        collectJob.cancel()
    }

    @Test
    fun `multiple collectors share the same StateFlow behavior`() = runTest {
        var callCount = 0
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getContentCardUIFlow()

        // Start two collectors
        val emissions1 = mutableListOf<Result<List<com.adobe.marketing.mobile.aepcomposeui.AepUI<*, *>>>>()
        val emissions2 = mutableListOf<Result<List<com.adobe.marketing.mobile.aepcomposeui.AepUI<*, *>>>>()

        val job1 = launch {
            flow.collect { emissions1.add(it) }
        }
        val job2 = launch {
            flow.collect { emissions2.add(it) }
        }

        // Process initial emissions
        advanceUntilIdle()
        assertTrue("Both collectors should have initial emissions", emissions1.size >= 1 && emissions2.size >= 1)

        // Trigger refresh with same data - should NOT cause new emissions
        val initialCount1 = emissions1.size
        val initialCount2 = emissions2.size
        contentCardUIProvider.refreshContent()
        advanceUntilIdle()

        // Both collectors should NOT receive new emissions since data is the same
        assertEquals("First collector should NOT receive new emission when data unchanged", initialCount1, emissions1.size)
        assertEquals("Second collector should NOT receive new emission when data unchanged", initialCount2, emissions2.size)

        // Verify that refreshContent actually called the API three times (onStart for each collector + manual refresh)
        // Note: onStart triggers for each collector, so 2 collectors + 1 manual refresh = 3 calls
        assertEquals("Should have called API three times", 3, callCount)

        job1.cancel()
        job2.cancel()
    }
}
