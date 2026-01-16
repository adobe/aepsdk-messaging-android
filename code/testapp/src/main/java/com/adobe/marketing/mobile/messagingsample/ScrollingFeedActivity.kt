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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.components.AepInbox
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepLazyColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.messaging.MessagingInboxProvider
import com.adobe.marketing.mobile.messaging.ContentCardEventObserver
import com.adobe.marketing.mobile.messaging.ContentCardUIEventListener
import com.adobe.marketing.mobile.messaging.Surface
import com.adobe.marketing.mobile.messagingsample.databinding.ActivityScrollingBinding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScrollingFeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScrollingBinding
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

        val viewModel: ExistingViewModel = ViewModelProvider(this)[ExistingViewModel::class.java]
        contentCardCallback = ContentCardCallback()

        val refreshButton: ImageButton = findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            Messaging.updatePropositionsForSurfaces(surfaces)
            viewModel.refresh()
        }

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val smallImageCardStyleColumn = SmallImageUIStyle.Builder()
                        .rootRowStyle(
                            AepRowStyle(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            )
                        )
                        .build()

                    val largeImageCardStyleColumn = LargeImageUIStyle.Builder()
                        .imageStyle(
                            AepImageStyle(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentScale = ContentScale.FillWidth
                            )
                        )
                        .textColumnStyle(AepColumnStyle(modifier = Modifier.padding(8.dp)))
                        .build()

                    val imageOnlyCardStyleColumn = ImageOnlyUIStyle.Builder()
                        .imageStyle(
                            AepImageStyle(
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        )
                        .build()

                    val rowCardStyle = AepCardStyle(
                        modifier = Modifier.width(400.dp).padding(8.dp),
                    )
                    val smallImageCardStyleRow = SmallImageUIStyle.Builder()
                        .cardStyle(rowCardStyle)
                        .rootRowStyle(
                            AepRowStyle(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp,
                                    Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            )
                        )
                        .build()

                    val largeImageCardStyleRow = LargeImageUIStyle.Builder()
                        .cardStyle(rowCardStyle)
                        .rootColumnStyle(
                            AepColumnStyle(
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        )
                        .build()

                    val imageOnlyCardStyleRow = ImageOnlyUIStyle.Builder()
                        .cardStyle(rowCardStyle)
                        .imageStyle(AepImageStyle(modifier = Modifier.fillMaxSize()))
                        .build()

                    val inboxUi = viewModel.inboxUIStateFlow.collectAsStateWithLifecycle().value
                    val cardUIStyle = AepUIStyle(
                        smallImageUIStyle = smallImageCardStyleRow,
                        largeImageUIStyle = largeImageCardStyleRow,
                        imageOnlyUIStyle = imageOnlyCardStyleRow,
                    )

                    val headingStyle = AepTextStyle(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    val inboxContainerStyle = InboxUIStyle.Builder()
                        .headingStyle(headingStyle)
                        .lazyColumnStyle(
                            AepLazyColumnStyle(
                                modifier = Modifier.background(Color.DarkGray),
                                contentPadding = PaddingValues(10.dp)
                            )
                        )
                        .build()

                    AepInbox(
                        uiState = inboxUi,
                        inboxStyle = inboxContainerStyle,
                        itemsStyle = cardUIStyle,
                        observer = ContentCardEventObserver(ContentCardCallback())
                    )
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
class ExistingViewModel: ViewModel() {
    private val containerUIProvider = MessagingInboxProvider(Surface("card/ms"))

    val inboxUIStateFlow = containerUIProvider.getInboxUI()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUIState.Loading
        )

    fun refresh() {
        viewModelScope.launch {
            containerUIProvider.refresh()
        }
    }
}