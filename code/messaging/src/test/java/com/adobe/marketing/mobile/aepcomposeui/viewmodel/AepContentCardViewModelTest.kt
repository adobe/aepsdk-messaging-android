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

package com.adobe.marketing.mobile.aepcomposeui.viewmodel

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import com.adobe.marketing.mobile.messaging.Surface
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AepContentCardViewModelTest {
    /*private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var surface: Surface
    private lateinit var contentCardUIProvider: ContentCardUIProvider
    private lateinit var viewModel: AepContentCardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)
        every { surface.isValid() } returns true
        every { surface.uri } returns "test://surface"
        every { surface.toEventData() } returns mapOf("uri" to "test://surface")
        contentCardUIProvider = ContentCardUIProvider(surface)
        viewModel = AepContentCardViewModel(contentCardUIProvider)
    }*/

    /*@After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getContentCardUI returns initial empty flow`() = runTest {
        viewModel.
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `test refreshContent updates content`() = runTest {
        contentCardUIProvider.refreshContent()
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.isSuccess)
    }

    @Test
    fun `test with invalid surface`() = runTest {
        every { surface.isValid() } returns false
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `test with empty surface uri`() = runTest {
        every { surface.uri } returns ""
        every { surface.toEventData() } returns mapOf("uri" to "")
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `test with null event data`() = runTest {
        every { surface.toEventData() } returns null
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `test with empty event data`() = runTest {
        every { surface.toEventData() } returns emptyMap()
        val result = contentCardUIProvider.getContentCardUI().first()
        assert(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun initial_state_has_empty_aep_ui_list() {
        //val mockContentCardUIProvider = mockk<ContentCardUIProvider>()
        val viewModel = AepContentCardViewModel(contentCardUIProvider)
        val initialList = viewModel.aepUIList.value
        assertTrue(initialList.isEmpty())
    }*/

    /*@get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var contentCardUIProvider: ContentCardUIProvider
    private lateinit var viewModel: AepContentCardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        contentCardUIProvider = mockk()
        viewModel = AepContentCardViewModel(contentCardUIProvider)
    }

    @Test
    fun `test getContentCardUI success path`() = runTest {
        val mockList = listOf<AepUI<*, *>>(mockk(), mockk())
        val successFlow = flow {
            emit(Result.success(mockList))
        }

        every { contentCardUIProvider.getContentCardUI() } returns successFlow

        viewModel = AepContentCardViewModel(contentCardUIProvider)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(mockList, viewModel.aepUIList.value)
        verify { contentCardUIProvider.getContentCardUI() }
    }

    @Test
    fun `test getContentCardUI failure path`() = runTest {
        val mockError = Exception("Test exception")
        val failureFlow = flow<Result<List<AepUI<*, *>>>> {
            emit(Result.failure(mockError))
        }

        every { contentCardUIProvider.getContentCardUI() } returns failureFlow

        viewModel = AepContentCardViewModel(contentCardUIProvider)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<AepUI<*, *>>(), viewModel.aepUIList.value)
        verify { contentCardUIProvider.getContentCardUI() }
    }

    @Test
    fun `test refreshContent`() = runTest {
        coEvery { contentCardUIProvider.refreshContent() } just Runs

        viewModel.refreshContent()

        coVerify { contentCardUIProvider.refreshContent() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }*/

    // Successful content fetch should update aepUIList with new AepUI items
    @Test
    fun successful_content_fetch_updates_aep_ui_list() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        val mockContentCardUIProvider = mockk<ContentCardUIProvider>()
        val mockAepUI = mockk<AepUI<*, *>>()
        val expectedList = listOf(mockAepUI)
        val flow = flowOf(Result.success(expectedList))

        coEvery { mockContentCardUIProvider.getContentCardUI() } returns flow

        val viewModel = AepContentCardViewModel(mockContentCardUIProvider)

        advanceUntilIdle()

        assertEquals(expectedList, viewModel.aepUIList.value)
    }
}

