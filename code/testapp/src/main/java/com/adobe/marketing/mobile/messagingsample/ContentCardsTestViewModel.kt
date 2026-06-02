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

package com.adobe.marketing.mobile.messagingsample

import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.messaging.ContentCardEventObserver
import com.adobe.marketing.mobile.messaging.ContentCardUIEventListener
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import com.adobe.marketing.mobile.messaging.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SurfaceCardSection(
    val pathToken: String,
    val surface: Surface,
    val provider: ContentCardUIProvider,
    val observer: ContentCardEventObserver,
)

class ContentCardsTestViewModel : ViewModel() {

    private val _sections = MutableStateFlow<List<SurfaceCardSection>>(emptyList())
    val sections: StateFlow<List<SurfaceCardSection>> = _sections.asStateFlow()

    fun setSurfaceEntries(entries: List<Pair<String, Surface>>) {
        _sections.value =
            entries.map { (path, surface) ->
                val provider = ContentCardUIProvider(surface)
                val observer =
                    ContentCardEventObserver(SampleContentCardEventCallback, provider)
                SurfaceCardSection(path, surface, provider, observer)
            }
    }

    private object SampleContentCardEventCallback : ContentCardUIEventListener {
        override fun onDisplay(aepUI: AepUI<*, *>) {}

        override fun onDismiss(aepUI: AepUI<*, *>) {}

        override fun onInteract(
            aepUI: AepUI<*, *>,
            interactionId: String?,
            actionUrl: String?
        ): Boolean = false
    }
}
