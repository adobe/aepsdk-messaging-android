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

package com.adobe.marketing.mobile.aepcomposeui.aepui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.SmallImageCard
import com.adobe.marketing.mobile.aepcomposeui.aepui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepuitemplates.SmallImageTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Composable for rendering a list of AEP UI components.
 * Maintains a list of AEP UI components and renders them using the provided container.
 */
@Composable
fun AepList(
    viewModel: AepListViewModel,
    aepUiEventObserver: AepUIEventObserver,
    aepUiStyle: AepUIStyle,
    container: @Composable (@Composable () -> Unit) -> Unit = { content ->
        // Default to a Column if no container is provided
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                content()
            }
        }
    }
) {
    val uiList = viewModel.uiList.collectAsStateWithLifecycle()

    container {
        uiList.value.forEach { ui ->
            val uiAsComposable = asComposable(ui, aepUiEventObserver, aepUiStyle)
            uiAsComposable.invoke()
        }
    }
}

private fun asComposable(
    aepUI: AepUI<*, *>,
    observer: AepUIEventObserver,
    aepUiStyle: AepUIStyle
): @Composable () -> Unit {
    return when (aepUI) {
        is SmallImageUI -> {
            {
                val state = aepUI.getState()
                if (!state.dismissed) {
                    SmallImageCard(
                        ui = aepUI,
                        style = aepUiStyle.smallImageUiStyle,
                        observer = observer
                    )
                }
            }
        }

        else -> throw IllegalArgumentException("Unknown template type")
    }
}

class AepListViewModel(
    private val contentProvider: AepUIContentProvider,
) : ViewModel() {

    private val _uiList = MutableStateFlow(listOf<AepUI<*, *>>())
    val uiList: StateFlow<List<AepUI<*, *>>> = _uiList

    init {
        viewModelScope.launch {
            contentProvider.getContent().collect { templates ->

                val uiList = templates.map { template ->

                    val aepUiState: AepUI<*, *> = when (template) {
                        is SmallImageTemplate -> SmallImageUI(
                            template,
                            SmallImageCardUIState(dismissed = false)
                        )

                        else -> throw IllegalArgumentException("Unknown template type")
                    }

                    aepUiState
                }
                _uiList.value = uiList
            }
        }
    }
}

class AepComposableViewModelFactory(
    private val contentProvider: AepUIContentProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AepListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AepListViewModel(contentProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
