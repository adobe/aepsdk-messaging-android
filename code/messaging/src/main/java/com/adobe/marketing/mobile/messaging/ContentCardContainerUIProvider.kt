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

package com.adobe.marketing.mobile.messaging

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepcomposeui.AepContainerUI
import com.adobe.marketing.mobile.aepcomposeui.InboxContainerUI
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepContainerUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.state.InboxContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepContainerUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxContainerUITemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/**
 * ContentCardContainerUIProvider is responsible for fetching and managing container UI templates
 * for a given surface. It manages the container configuration and provides reactive
 * updates when the container needs to be refreshed.
 *
 * @property contentCardUIProvider The provider for the content cards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContentCardContainerUIProvider(val surface: Surface) : AepContainerUIContentProvider {

    private val contentCardUIProvider = ContentCardUIProvider(surface)

    // Reactive state flow that holds the current container UI template (null = not loaded yet)
    private val _containerUIFlow = MutableStateFlow<Result<AepContainerUITemplate>?>(null)
    private val containerUIFlow = _containerUIFlow.asStateFlow()

    // Transformed flow that starts with container and switches to content cards with flatMapLatest
    private fun toAepContainerUI(): Flow<Result<AepContainerUI<*, *>>> =
        containerUIFlow.filterNotNull().flatMapLatest { containerResult: Result<AepContainerUITemplate> ->
            flow {
                val containerUI = containerResult.getOrNull()
                if (containerUI != null) {
                    // Immediately emit loading state
                    when (containerUI) {
                        is InboxContainerUITemplate -> {
                            emit(
                                Result.success(
                                    InboxContainerUI(containerUI, InboxContainerUIState.Loading)
                                )
                            )
                        }
                    }

                    // Then collect content cards and emit updates
                    contentCardUIProvider.getUIContent().collect { contentCardResult ->
                        // Convert AepUITemplate list to AepUI list
                        val aepUIList = contentCardResult.getOrNull()?.mapNotNull { template ->
                            ContentCardSchemaDataUtils.getAepUI(template)
                        }

                        when (containerUI) {
                            is InboxContainerUITemplate -> {
                                if (aepUIList != null) {
                                    emit(
                                        Result.success(
                                            InboxContainerUI(
                                                containerUI,
                                                InboxContainerUIState.Success(aepUIList)
                                            )
                                        )
                                    )
                                } else {
                                    emit(
                                        Result.success(
                                            InboxContainerUI(
                                                containerUI,
                                                InboxContainerUIState.Error(
                                                    contentCardResult.exceptionOrNull()
                                                        ?: Throwable("Unknown error loading container content cards")
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    emit(
                        Result.failure(
                            containerResult.exceptionOrNull() ?: Throwable("Container not loaded yet")
                        )
                    )
                }
            }
        }

    fun getContentCardContainerUI(): Flow<Result<AepContainerUI<*, *>>> =
        toAepContainerUI().onStart {
            refreshContainer()
        }

    /**
     * Retrieves a reactive flow of container UI templates.
     *
     * The flow automatically loads the initial container template when first collected,
     * then continues to emit updates whenever [refreshContainer] is called.
     *
     * All collectors will automatically receive the loaded content and any future updates.
     *
     * @return A [Flow] that emits a [Result] containing the [AepContainerUITemplate].
     */
    override fun getContainerUIContent(): Flow<Result<AepContainerUITemplate>> =
        containerUIFlow
            .onStart {
                // Only fetch if not already loaded (lazy loading)
                if (_containerUIFlow.value == null) {
                    refreshContainer()
                }
            }
            // Only emit actual results, filter out null (loading) states
            .filterNotNull()

    /**
     * Refreshes the container UI by fetching new container configuration and updating
     * the flow returned by [getContainerUIContent]. This will cause all collectors of the flow
     * to receive the updated container.
     *
     * Note: [getContainerUIContent] automatically loads initial content when first collected,
     * so this method is only needed for manual refresh operations.
     */
    override suspend fun refreshContainer() {
        _containerUIFlow.value = fetchContainer()
        contentCardUIProvider.refreshContent()
    }

    /**
     * Fetches the container configuration. This method will be updated in the future
     * to call an API similar to Messaging.getPropositionsForSurface.
     *
     * @return A [Result] containing the [AepContainerUITemplate] or an error if fetching fails.
     */
    private fun fetchContainer(): Result<AepContainerUITemplate> {
        return Result.success(
            InboxContainerUITemplate(
                heading = AepText("Message Inbox"),
                capacity = 15,
                emptyMessage = AepText("No messages right now"),
                unreadIcon = AepImage(
                    url = "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png",
                    darkUrl = "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png",
                ),
                unreadBgColor = AepColor(Color.DarkGray, Color.LightGray),
                unreadIconAlignment = Alignment.TopStart
            )
        )
    }
}
