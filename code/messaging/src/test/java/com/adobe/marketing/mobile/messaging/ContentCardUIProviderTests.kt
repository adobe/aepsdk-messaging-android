package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.util.JSONUtils
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
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

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ContentCardUIProviderTests {

    private lateinit var contentCardUIProvider: ContentCardUIProvider
    private lateinit var surface: Surface
    private lateinit var mockMessaging: MockedStatic<Messaging>
    private lateinit var contentCardSchemaData: ContentCardSchemaData
    private lateinit var propositionItem: PropositionItem
    private lateinit var proposition: Proposition

    private val baseJson = JSONObject("""
        {
          "id": "1c1fb7c4-f3e7-4766-a782-ec5b3c87a62e",
          "type": "schema",
          "detail": {
            "id": "1c1fb7c4-f3e7-4766-a782-ec5b3c87a62e",
            "schema": "https://ns.adobe.com/personalization/message/content-card",
            "data": {
              "content": {
                "actionUrl": "",
                "body": {
                  "content": "Content card testing triggers track action \"smoke_test\""
                },
                "buttons": [
                  {
                    "interactId": "buttonID1",
                    "actionUrl": "https://adobe.com/offer",
                    "text": {
                      "content": "Purchase Now"
                    }
                  }
                ],
                "image": {
                  "alt": "",
                  "url": "https://i.ibb.co/0X8R3TG/Messages-24.png"
                },
                "dismissBtn": {
                  "style": "none"
                },
                "title": {
                  "content": "Messaging SDK Smoke Test"
                }
              },
              "contentType": "application/json",
              "meta": {
                "adobe": {
                  "template": "SmallImage"
                },
                "surface": "mobileapp://com.adobe.marketing.mobile.messagingsample/card/ms"
              }
            }
          }
        }
    """)

    private val baseJsonMap = JSONUtils.toMap(baseJson) ?: emptyMap()
    private val contentMap = (baseJsonMap["detail"] as? Map<*, *>)?.get("data") as? Map<String, Any>
    private val metaMap = contentMap?.get("meta") as? Map<String, Any>
    private val contentCardMap = contentMap?.get("content") as? Map<String, Any> ?: emptyMap()

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
            // Immediately trigger callback with test data
            callback.call(mapOf(surface to listOf(proposition)))
        }

        // Create and collect the flow
        val flow = contentCardUIProvider.getContentCardUI()
        val result = flow.first()

        // Verify the result
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
}