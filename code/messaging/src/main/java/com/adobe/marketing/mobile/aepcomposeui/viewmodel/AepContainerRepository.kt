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

package com.adobe.marketing.mobile.aepcomposeui.viewmodel

import com.adobe.marketing.mobile.aepcomposeui.AepContainerUI
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepContainerUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepContainerUITemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

sealed interface AepContainerState {
    object Loading : AepContainerState
    data class Success(val containerUI: AepContainerUI<*, *>) : AepContainerState
    data class Error(val error: Throwable) : AepContainerState
}

/**
 * Repository class responsible for managing the state of the container UI and its associated content cards.
 * It interacts with both the [AepUIContentProvider] and the [AepContainerUIContentProvider] to fetch and refresh data.
 */
class AepContainerRepository(
    private val aepUIProvider: AepUIContentProvider,
    private val aepContainerUIProvider: AepContainerUIContentProvider
) {

    private val _aepContainerUiState = MutableStateFlow<AepContainerState>(AepContainerState.Loading)
    val aepContainerState: StateFlow<AepContainerState> = _aepContainerUiState.asStateFlow()

    /**
     * Loads the container UI along with its associated content cards and updates the state accordingly.
     * The state can be one of the following:
     * - [AepContainerState.Loading]: Indicates that the loading process is in progress.
     * - [AepContainerState.Success]: Indicates that the container UI and content cards were loaded successfully
     * and contains the loaded [AepContainerUI].
     * - [AepContainerState.Error]: Indicates that an error occurred during the loading process
     * and contains the associated [Throwable].
     *
     * This function collects data from both the [AepUIContentProvider] and the [AepContainerUIContentProvider].
     * It combines the results to create a comprehensive state representation for the container UI.
     */
    suspend fun loadContainerUI() {
        _aepContainerUiState.value = AepContainerState.Loading
        
        // Combine both flows to react to changes from either provider
        combine(
            aepUIProvider.getContentCardUIFlow(),
            aepContainerUIProvider.getContainerUIFlow()
        ) { contentCardResult, containerResult ->
            createContainerState(contentCardResult, containerResult)
        }.collect { state ->
            _aepContainerUiState.value = state
        }
    }
    
    /**
     * Manually refreshes the container UI by triggering a refresh on the content providers.
     * This will cause the combined flow to emit new values automatically.
     */
    suspend fun refreshContainerUI() {
        _aepContainerUiState.value = AepContainerState.Loading
        aepUIProvider.refreshContent()
        aepContainerUIProvider.refreshContainerUI()
    }
    
    private fun createContainerState(
        contentCardResult: Result<List<AepUI<*, *>>>,
        containerResult: Result<AepContainerUITemplate>
    ): AepContainerState {
        // todo uncomment once required models are implemented
        return AepContainerState.Loading
//        val containerUI = containerResult.getOrNull()
//        if (containerUI != null) {
//            return when (containerUI) {
//                is InboxContainerUITemplate -> {
//                    AepContainerState.Success(
//                        InboxContainerUI(
//                            containerUI,
//                            InboxContainerUIState(
//                                aepUIList = contentCardResult.getOrDefault(emptyList())
//                            )
//                        )
//                    )
//                }
//
//                is CarouselContainerUITemplate -> {
//                    if (contentCardResult.isFailure) {
//                        AepContainerState.Error(contentCardResult.exceptionOrNull()!!)
//                    } else {
//                        AepContainerState.Success(
//                            CarouselContainerUI(
//                                containerUI,
//                                CarouselContainerUIState(
//                                    contentCardResult.getOrDefault(emptyList())
//                                )
//                            )
//                        )
//                    }
//                }
//            }
//        } else {
//            return AepContainerState.Error(
//                containerResult.exceptionOrNull()
//                    ?: Throwable("Unknown error loading container UI")
//            )
//        }
    }
}