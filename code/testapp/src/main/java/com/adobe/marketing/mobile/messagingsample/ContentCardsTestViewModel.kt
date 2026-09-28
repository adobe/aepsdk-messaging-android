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

    /** Human-readable status shown below the action buttons (e.g. "Loaded 3 card(s) from Disk"). */
    private val _loadStatus = MutableStateFlow("")
    val loadStatus: StateFlow<String> = _loadStatus.asStateFlow()

    /** Replace all sections with the given list of surface entries. */
    fun setSurfaceEntries(entries: List<Pair<String, Surface>>) {
        _sections.value =
            entries.map { (path, surface) ->
                val provider = ContentCardUIProvider(surface)
                val observer =
                    ContentCardEventObserver(SampleContentCardEventCallback, provider)
                SurfaceCardSection(path, surface, provider, observer)
            }
    }

    /**
     * Add or replace the section for a single surface, leaving other surfaces unchanged.
     * If a section with the same path already exists it is refreshed in place; otherwise
     * it is appended.
     */
    fun upsertSurfaceEntry(path: String, surface: Surface) {
        val provider = ContentCardUIProvider(surface)
        val observer = ContentCardEventObserver(SampleContentCardEventCallback, provider)
        val newSection = SurfaceCardSection(path, surface, provider, observer)
        val current = _sections.value.toMutableList()
        val idx = current.indexOfFirst { it.pathToken == path }
        if (idx >= 0) current[idx] = newSection else current.add(newSection)
        _sections.value = current
    }

    /** Remove the section for a single surface path, if present. */
    fun removeSurfaceEntry(path: String) {
        _sections.value = _sections.value.filterNot { it.pathToken == path }
    }

    fun setLoadStatus(status: String) {
        _loadStatus.value = status
    }

    fun clearSections() {
        _sections.value = emptyList()
        _loadStatus.value = ""
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
