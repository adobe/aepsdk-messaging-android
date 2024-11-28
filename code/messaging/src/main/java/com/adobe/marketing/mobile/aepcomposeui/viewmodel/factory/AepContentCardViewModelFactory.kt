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
