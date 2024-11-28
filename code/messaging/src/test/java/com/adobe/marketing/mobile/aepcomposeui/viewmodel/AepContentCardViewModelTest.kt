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

import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import com.adobe.marketing.mobile.messaging.Surface
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AepContentCardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getContentCardUI returns initial empty flow`() = runTest {
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
}
