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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.aepcomposeui.AepContainerUI
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.InboxContainerUI
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepContainerUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.state.InboxContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxContainerUITemplate
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AepContainerViewModel(
    private val aepUIProvider: AepUIContentProvider,
    private val aepContainerUIProvider: AepContainerUIContentProvider
) : ViewModel() {

    private val _uiList = MutableStateFlow(emptyList<AepUI<*, *>>())
    val uiList: StateFlow<List<AepUI<*, *>>> = _uiList

    private val _containerUI = MutableStateFlow<AepContainerUI<*, *>?>(null)
    val containerUI: StateFlow<AepContainerUI<*, *>?> = _containerUI

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    init {
        viewModelScope.launch {
            // First, collect UI content changes and update the UI list
            aepUIProvider.getContent().collect { result ->
                if (result.isSuccess) {
                    result.getOrNull()?.let { templateList ->
                        _uiList.value = templateList.mapNotNull { item ->
                            UIUtils.getAepUI(item)
                        }
                    }
                }
                _isLoaded.value = true
            }
        }

        viewModelScope.launch {
            combine(
                uiList,
                aepContainerUIProvider.getContainerUI(),
                isLoaded
            ) { currentUIList, containerResult, loaded ->
                containerResult.getOrNull()?.let { containerTemplate ->
                    // create different types of container UI here based on template type
                    null
                }
            }.collect { containerUI ->
                _containerUI.value = containerUI
            }
        }
    }

    fun refreshContent() {
        viewModelScope.launch {
            _isLoaded.value = false
            aepUIProvider.refreshContent()
        }
    }
}

class AepContainerViewModelFactory(
    private val aepUIProvider: AepUIContentProvider,
    private val aepContainerUIProvider: AepContainerUIContentProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AepContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AepContainerViewModel(aepUIProvider, aepContainerUIProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
