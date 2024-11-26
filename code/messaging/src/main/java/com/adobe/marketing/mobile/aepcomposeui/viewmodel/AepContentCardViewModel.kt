package com.adobe.marketing.mobile.aepcomposeui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AepContentCardViewModel(private val contentCardUIProvider: ContentCardUIProvider) : ViewModel() {
    // State to hold AepUI list
    private val _aepUIList = MutableStateFlow<List<AepUI<*, *>>>(emptyList())
    val aepUIList: StateFlow<List<AepUI<*, *>>> = _aepUIList.asStateFlow()

    init {
        // Launch a coroutine to fetch the aepUIList from the ContentCardUIProvider
        // when the ViewModel is created
        viewModelScope.launch {
            contentCardUIProvider.getContentCardUI().collect { aepUiResult ->
                aepUiResult.onSuccess { aepUi ->
                    _aepUIList.value = aepUi
                }
                aepUiResult.onFailure { throwable ->
                    Log.d("ContentCardUIProvider", "Error fetching AepUI list: ${throwable}")
                }
            }
        }
    }

    // Function to refresh the aepUIList from the ContentCardUIProvider
    fun refreshContent() {
        viewModelScope.launch {
            contentCardUIProvider.refreshContent()
        }
    }
}