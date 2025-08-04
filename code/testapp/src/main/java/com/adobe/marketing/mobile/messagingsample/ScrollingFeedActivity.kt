/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messagingsample

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.components.ImageOnlyCard
import com.adobe.marketing.mobile.aepcomposeui.components.LargeImageCard
import com.adobe.marketing.mobile.aepcomposeui.components.SmallImageCard
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepIconStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.messaging.ContentCardEventObserver
import com.adobe.marketing.mobile.messaging.ContentCardMapper
import com.adobe.marketing.mobile.messaging.ContentCardUIEventListener
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import com.adobe.marketing.mobile.messaging.Surface
import com.adobe.marketing.mobile.messagingsample.databinding.ActivityScrollingBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScrollingFeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScrollingBinding
    private lateinit var contentCardUIProvider: ContentCardUIProvider
    private lateinit var contentCardViewModel: AepContentCardViewModel
    private lateinit var contentCardCallback: ContentCardCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // staging environment - CJM Stage, AJO Web (VA7)
        // surface for content card -
        // mobileapp://com.adobe.marketing.mobile.messagingsample/card/ms
        val surfaces = mutableListOf<Surface>()
        val surface = Surface("card/ms")
        surfaces.add(surface)

        // Initialize the ContentCardUIProvider
        contentCardUIProvider = ContentCardUIProvider(surface)

        // Initialize the ViewModel
        contentCardViewModel =
            ViewModelProvider(this, AepContentCardViewModelFactory(contentCardUIProvider)).get(
                AepContentCardViewModel::class.java
            )

        contentCardCallback = ContentCardCallback()

        // Set a click listener for refresh button which calls the API for fetch content cards from Edge
        val refreshButton: ImageButton = findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            Messaging.updatePropositionsForSurfaces(surfaces)
            contentCardViewModel.refreshContent()
        }

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AepContentCardList(contentCardViewModel)
                }
            }
        }
    }


    @Composable
    private fun AepContentCardList(viewModel: AepContentCardViewModel) {
        // Collect the state from ViewModel
        val aepUiList by viewModel.aepUIList.collectAsStateWithLifecycle()

        // Get the ContentCardSchemaData for the AepUI list if needed
        val contentCardSchemaDataList = aepUiList.map {
            when (it) {
                is SmallImageUI ->
                    ContentCardMapper.Companion.instance.getContentCardSchemaData(it.getTemplate().id)

                else -> null
            }
        }

        // Reorder the AepUI list based on the ContentCardSchemaData fields if needed
        val reorderedAepUIList = aepUiList.sortedWith(compareByDescending {
            val rank =
                contentCardSchemaDataList[aepUiList.indexOf(it)]?.meta?.get("priority") as String?
                    ?: "0"
            rank.toInt()
        })

        // Displaying content cards in a Column
        // create a custom style for the small image card in column
        val smallImageCardStyleColumn = SmallImageUIStyle.Builder()
            .rootRowStyle(
                AepRowStyle(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                )
            )
            .build()

        val largeImageCardStyleColumn = LargeImageUIStyle.Builder()
            .imageStyle(AepImageStyle(modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit))
            .textColumnStyle(AepColumnStyle(modifier =  Modifier.padding(8.dp)))
            .buttonRowStyle(
                AepRowStyle(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                )
            )
            .build()

        val imageOnlyCardStyleColumn = ImageOnlyUIStyle.Builder()
            .cardStyle(
                AepCardStyle(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ))
            .imageStyle(
                AepImageStyle(
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            )
            .build()

        // Create column with composables from AepUI instances
//        LazyColumn {
//            items(reorderedAepUIList) { aepUI ->
//                when (aepUI) {
//                    is SmallImageUI -> {
//                        val state = aepUI.getState()
//                        if (!state.dismissed) {
//                            SmallImageCard(
//                                ui = aepUI,
//                                style = smallImageCardStyleColumn,
//                                observer = ContentCardEventObserver(null)
//                            )
//                        }
//                    }
//                    is LargeImageUI -> {
//                        val state = aepUI.getState()
//                        if (!state.dismissed) {
//                            LargeImageCard(
//                                ui = aepUI,
//                                style = largeImageCardStyleColumn,
//                                observer = ContentCardEventObserver(contentCardCallback)
//                            )
//                        }
//                    }
//                    is ImageOnlyUI -> {
//                        val state = aepUI.getState()
//                        if (!state.dismissed) {
//                            ImageOnlyCard(
//                                ui = aepUI,
//                                style = imageOnlyCardStyleColumn,
//                                observer = ContentCardEventObserver(contentCardCallback)
//                            )
//                        }
//                    }
//                }
//            }
//        }

        // Displaying content cards in a Row
        // create a custom style for the small image card in row
        val smallImageCardStyleRow = SmallImageUIStyle.Builder()
            .cardStyle(AepCardStyle(modifier = Modifier.width(400.dp).height(200.dp).padding(8.dp)))
            .rootRowStyle(
                AepRowStyle(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            )
            .bodyAepTextStyle(AepTextStyle(maxLines = 3))
            .build()

        val largeImageCardStyleRow = LargeImageUIStyle.Builder()
            .cardStyle(AepCardStyle(modifier = Modifier.width(400.dp).height(200.dp).padding(8.dp)))
            .build()

        val imageOnlyCardStyleRow = ImageOnlyUIStyle.Builder()
            .cardStyle(AepCardStyle(modifier = Modifier.width(400.dp).height(200.dp).padding(8.dp)))
            .imageStyle(AepImageStyle(modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth))
            .build()

        // Create row with composables from AepUI instances
        LazyRow {
            items(reorderedAepUIList) { aepUI ->
                when (aepUI) {
                    is SmallImageUI -> {
                        val state = aepUI.getState()
                        if (!state.dismissed) {
                            SmallImageCard(
                                ui = aepUI,
                                style = smallImageCardStyleRow,
                                observer = ContentCardEventObserver(contentCardCallback)
                            )
                        }
                    }
                    is LargeImageUI -> {
                        val state = aepUI.getState()
                        if (!state.dismissed) {
                            LargeImageCard(
                                ui = aepUI,
                                style = largeImageCardStyleRow,
                                observer = ContentCardEventObserver(contentCardCallback)
                            )
                        }
                    }
                    is ImageOnlyUI -> {
                        val state = aepUI.getState()
                        if (!state.dismissed) {
                            ImageOnlyCard(
                                ui = aepUI,
                                style = imageOnlyCardStyleRow,
                                observer = ContentCardEventObserver(contentCardCallback)
                            )
                        }
                    }
                }
            }
        }
    }
}

class ContentCardCallback: ContentCardUIEventListener {
    override fun onDisplay(aepUI: AepUI<*, *>) {
        Log.d("ContentCardCallback", "onDisplay")
    }

    override fun onDismiss(aepUI: AepUI<*, *>) {
        Log.d("ContentCardCallback", "onDismiss")
    }

    override fun onInteract(
        aepUI: AepUI<*, *>,
        interactionId: String?,
        actionUrl: String?
    ): Boolean {
        Log.d("ContentCardCallback", "onInteract $interactionId $actionUrl")
        // If the url is handled here, return true
        return false
    }
}

// create new view model or reuse existing one to hold the aepUIList
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