package com.adobe.marketing.mobile.aepcomposeui.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adobe.marketing.mobile.aepcomposeui.viewmodel.AepContentCardViewModel
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider

class AepContentCardViewModelFactory(
    private val contentCardUIProvider: ContentCardUIProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AepContentCardViewModel::class.java) -> {
                AepContentCardViewModel(contentCardUIProvider) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}