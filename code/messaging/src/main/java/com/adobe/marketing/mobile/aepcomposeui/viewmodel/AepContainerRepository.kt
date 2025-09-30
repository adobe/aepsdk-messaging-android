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
import com.adobe.marketing.mobile.aepcomposeui.InboxContainerUI
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepContainerUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.state.InboxContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxContainerUITemplate
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

sealed interface AepContainerUiState {
    object Loading : AepContainerUiState
    data class Success(val containerUI: AepContainerUI<*, *>) : AepContainerUiState
    data class Error(val error: Throwable) : AepContainerUiState
}

class AepContainerRepository(
    private val aepUIProvider: AepUIContentProvider,
    private val aepContainerUIProvider: AepContainerUIContentProvider
) {
    private val _containerUiState = MutableStateFlow<AepContainerUiState>(AepContainerUiState.Loading)
    val containerUiState: StateFlow<AepContainerUiState> = _containerUiState.asStateFlow()

    suspend fun refreshContainer() {
        _containerUiState.update { AepContainerUiState.Loading }

        aepUIProvider.getContent().flatMapLatest { contentResult ->
            aepContainerUIProvider.getContainerUI().flatMapLatest { containerResult ->
                flowOf(
                    containerResult.getOrNull()?.let { containerTemplate ->
                        when (containerTemplate) {
                            is InboxContainerUITemplate -> {
                                val uiList = if (contentResult.isSuccess) {
                                    contentResult.getOrNull()?.mapNotNull { item ->
                                        UIUtils.getAepUI(item)
                                    } ?: emptyList()
                                } else {
                                    emptyList()
                                }
                                AepContainerUiState.Success(
                                    InboxContainerUI(
                                        containerTemplate,
                                        InboxContainerUIState(
                                            aepUIList = uiList
                                        )
                                    )
                                )
                            }
                        }
                    } ?: AepContainerUiState.Error(containerResult.exceptionOrNull() ?: Exception("Failed to load container UI, empty container template"))
                )
            }
        }.collect { state ->
            _containerUiState.update { state }
        }
    }
}
