package com.adobe.marketing.mobile.messaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adobe.marketing.mobile.messaging.MessagingConstants.CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import com.adobe.marketing.mobile.services.caching.CacheResult
import com.adobe.marketing.mobile.services.caching.CacheService
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContentCardImageManagerTests {

    @Mock private lateinit var mockCacheService: CacheService

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Mock
    private lateinit var mockNetworkService: Networking

    private lateinit var contentCardImageManager: ContentCardImageManager
    private lateinit var testCachePath: String
    private lateinit var testImageBitmapName: String
    private val imageUrl = "https://fastly.picsum.photos/id/43/400/300.jpg?hmac=fAPJ5p1wbFahFpnqtg004Nny-vTEADhmMxMkwLUSfw0"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        whenever(mockCacheService.set(any(), any(), any())).thenReturn(true)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)

        contentCardImageManager = ContentCardImageManager()
        testCachePath = CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
        testImageBitmapName = "sampleBitmapContentCard"
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
        Mockito.validateMockitoUsage()
    }

    @Test
    fun get_image_first_time_when_not_in_cache() {
        //Mocking Cache to bypass cache check
        whenever(mockCacheService.get(
            any(),
            any()
        )).thenReturn(null)

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        contentCardImageManager.getContentCardImageBitmap(imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            })

        mockedStaticBitmapFactory.close()
    }

    @Test
    fun getContentCardImageBitmap_getCachedImage() {

        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        //Mocking Cache to return a valid cache result
        whenever(mockCacheService.get(
            any(),
            any()
        )).thenReturn(object: CacheResult {
            override fun getData(): InputStream {
                return bitmapToInputStream(mockBitmap)
            }
            override fun getExpiry(): CacheExpiry {
                return CacheExpiry.never()
            }
            override fun getMetadata(): Map<String, String> {
                return emptyMap()
            }
        })

        contentCardImageManager.getContentCardImageBitmap(imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            })

        mockedStaticBitmapFactory.close()
    }

    private fun bitmapToInputStream(bitmap: Bitmap): ByteArrayInputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return ByteArrayInputStream(byteArray)
    }

    private fun simulateNetworkResponse(
        responseCode: Int,
        responseStream: InputStream?,
        metadata: Map<String, String>
    ): HttpConnecting {
        val mockResponse = mock(HttpConnecting::class.java)
        `when`(mockResponse.responseCode).thenReturn(responseCode)
        `when`(mockResponse.inputStream).thenReturn(responseStream)
        `when`(mockResponse.getResponsePropertyValue(org.mockito.kotlin.any())).then {
            return@then metadata[it.getArgument(0)]
        }
        return mockResponse
    }
}