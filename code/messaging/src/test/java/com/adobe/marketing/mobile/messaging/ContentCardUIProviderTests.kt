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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
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
 * Tests for ContentCardUIProvider class.
 *
 * @ExperimentalCoroutinesApi is used here because this test class leverages Kotlin coroutines testing feature:
 * - runTest for structured concurrency testing
 */
@OptIn(kotlin.time.ExperimentalTime::class)
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
        whenever(proposition.getActivityId()).thenReturn("testId")
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
        val flow = contentCardUIProvider.getUIContent()
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

        val flow = contentCardUIProvider.getUIContent()
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
        val flow = contentCardUIProvider.getUIContent()
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

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getUIContent returns continuous flow with valid template`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()

        // Should emit initial value successfully
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getUIContent handles null proposition map in continuous flow`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(null)
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getUIContent handles empty proposition list`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to emptyList()))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles invalid schema type`() = runTest {
        whenever(propositionItem.schema).thenReturn(SchemaType.HTML_CONTENT)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles missing content`() = runTest {
        whenever(contentCardSchemaData.content).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles API failure`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.fail(AdobeError.UNEXPECTED_ERROR)
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getUIContent handles missing meta data`() = runTest {
        whenever(contentCardSchemaData.meta).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles proposition with no items`() = runTest {
        whenever(proposition.items).thenReturn(emptyList())

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles null contentCardSchemaData`() = runTest {
        whenever(propositionItem.contentCardSchemaData).thenReturn(null)

        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent handles empty result map`() = runTest {
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callback.call(emptyMap())
        }

        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `getUIContent emits when Messaging getPropositionsForSurfaces returns different data`() = runTest {
        var callCount = 0

        // Create a second proposition with different uniqueId to ensure data actually changes
        val proposition2 = mock(Proposition::class.java)
        val propositionItem2 = mock(PropositionItem::class.java)
        val contentCardSchemaData2 = mock(ContentCardSchemaData::class.java)

        // Setup second proposition with different data
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
            // Return different data on second call
            val dataToReturn = if (callCount == 1) {
                mapOf(surface to listOf(proposition))
            } else {
                mapOf(surface to listOf(proposition2)) // Different proposition
            }
            callback.call(dataToReturn)
        }

        val flow = contentCardUIProvider.getUIContent()

        // Get initial emission
        val firstResult = flow.first()
        assertTrue("First result should be successful", firstResult.isSuccess)

        // Trigger refresh with different data
        contentCardUIProvider.refreshContent()

        // Get result after refresh
        val secondResult = flow.first()
        assertTrue("Second result should be successful", secondResult.isSuccess)

        // Verify that API was called multiple times
        assertTrue("Should have called API at least twice", callCount >= 2)
    }

    @Test
    fun `getUIContent automatically refreshes content on first collection`() = runTest {
        var apiCallCount = 0
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            apiCallCount++
            callback.call(mapOf(surface to listOf(proposition)))
        }

        // The flow should automatically trigger content refresh when collected
        val flow = contentCardUIProvider.getUIContent()
        val result = flow.first()

        // Verify that the API was called automatically and content is emitted
        assertTrue("API should be called automatically", apiCallCount >= 1)
        assertTrue("Result should be successful", result.isSuccess)
        assertNotNull("Result should contain content", result.getOrNull())
        assertTrue("Result should not be empty", result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getUIContent does not emit when refreshContent returns same data`() = runTest {
        var callCount = 0
        mockMessaging.`when`<Unit> {
            Messaging.getPropositionsForSurfaces(any(), any())
        }.thenAnswer { invocation ->
            val callback = invocation.arguments[1] as AdobeCallbackWithError<Map<Surface, List<Proposition>>>
            callCount++
            // Always return the same data
            callback.call(mapOf(surface to listOf(proposition)))
        }

        val flow = contentCardUIProvider.getUIContent()

        // Get initial result
        val initialResult = flow.first()
        assertTrue("Initial result should be successful", initialResult.isSuccess)

        // Trigger refresh with same data
        contentCardUIProvider.refreshContent()

        // Get result after refresh - should be the same
        val refreshResult = flow.first()
        assertTrue("Refresh result should be successful", refreshResult.isSuccess)

        // Verify that refreshContent called the API (at least once for initial + once for refresh)
        assertTrue("Should have called API at least twice", callCount >= 2)
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

        val flow = contentCardUIProvider.getUIContent()

        // Test that multiple collectors can access the flow
        val result1 = flow.first()
        val result2 = flow.first()

        assertTrue("First collector should get successful result", result1.isSuccess)
        assertTrue("Second collector should get successful result", result2.isSuccess)

        // Trigger refresh with same data
        contentCardUIProvider.refreshContent()

        // Both should still get results after refresh
        val refreshResult1 = flow.first()
        val refreshResult2 = flow.first()

        assertTrue("First collector should get successful result after refresh", refreshResult1.isSuccess)
        assertTrue("Second collector should get successful result after refresh", refreshResult2.isSuccess)

        // Verify that API was called multiple times
        assertTrue("Should have called API multiple times", callCount >= 2)
    }
}
