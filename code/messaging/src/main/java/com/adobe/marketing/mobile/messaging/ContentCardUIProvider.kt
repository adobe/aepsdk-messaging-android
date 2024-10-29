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

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaDataUtils.buildTemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaDataUtils.getAepUI
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ContentCardUiProvider is responsible for fetching and managing the content for a given surface.
 * It uses Adobe Messaging APIs to retrieve propositions and transform them into UI templates for display.
 *
 * @property surfaceString The identifier of the surface for which content needs to be fetched.
 */
class ContentCardUIProvider(val surfaceString: String) : AepUIContentProvider {
    companion object {
        private const val SELF_TAG: String = "ContentCardUIProvider"
    }

    private val _contentFlow = MutableStateFlow<List<AepUITemplate>>(emptyList())
    private val contentFlow: StateFlow<List<AepUITemplate>> = _contentFlow

    private val _aepUiFlow = MutableStateFlow<List<AepUI<*, *>>>(emptyList())
    private val aepUiFlow: StateFlow<List<AepUI<*, *>>> = _aepUiFlow

    /**
     * Retrieves a flow of AepUI instances for the given surface.
     *
     * This function initiates the content fetch using [getContent] and then returns a flow of
     * [AepUI] instances that represent the UI templates. The flow emits updates whenever new
     * content is fetched or any changes occur.
     *
     * @return A [Flow] that emits a list of [AepUI] instances.
     */
    suspend fun getAepUi(): Flow<List<AepUI<*, *>>> {
        getContent()
        return aepUiFlow
    }

    /**
     * Clears the current content and re-fetches new content for the given surface.
     */
    override suspend fun refreshContent() {
        _contentFlow.value = emptyList()
        _aepUiFlow.value = emptyList()
        getContent()
    }

    /**
     * Retrieves a flow of AepUITemplate lists for the given surface.
     * The flow emits updates whenever new content is fetched.
     *
     * @return A flow that emits lists of AepUITemplate.
     */
    override suspend fun getContent(): Flow<List<AepUITemplate>> {
        CoroutineScope(Dispatchers.IO).launch {
            val surface = Surface(surfaceString)
            getAepUITemplateList(surface) { it ->
                it.onSuccess { templateList ->
                    _contentFlow.value = templateList
                    _aepUiFlow.value = templateList.mapNotNull { item -> getAepUI(item) }
                }
                it.onFailure { error ->
                    Log.error(
                        MessagingConstants.LOG_TAG, SELF_TAG,
                        "Failed to get content: ${error.message}"
                    )
                    _contentFlow.value = emptyList()
                    _aepUiFlow.value = emptyList()
                }
            }
        }
        return contentFlow
    }

    private suspend fun getAepUITemplateList(
        surface: Surface,
        completion: (Result<List<AepUITemplate>>) -> Unit
    ) {
        val surfaceList = mutableListOf<Surface>()
        surfaceList.add(surface)
        // Retrieve propositions for the provided surface
        Messaging.getPropositionsForSurfaces(
            surfaceList,
            object :
                AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
                override fun call(resultMap: Map<Surface, List<Proposition>>?) {
                    if (resultMap == null) {
                        completion(
                            Result.failure(
                                Throwable(
                                    "resultMap null for surfaces ${surfaceList.joinToString(",")}"
                                )
                            )
                        )
                        return
                    }

                    val templateModelList = mutableListOf<AepUITemplate>()
                    Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "getPropositionsForSurfaces callback contained Null Map"
                    )

                    for ((_, propositions) in resultMap.entries) {
                        for (proposition in propositions) {
                            try {
                                val aepUiTemplate = buildTemplate(proposition)
                                aepUiTemplate?.let { templateModelList.add(it) }
                            } catch (e: IllegalArgumentException) {
                                Log.error(
                                    MessagingConstants.LOG_TAG,
                                    SELF_TAG,
                                    "Failed to build template: ${e.message}"
                                )
                            }
                        }
                    }
                    completion(Result.success(templateModelList))
                }

                override fun fail(error: AdobeError?) {
                    completion(
                        Result.failure(
                            Throwable(
                                "Failed to retrieve propositions for surface ${
                                surfaceList.joinToString(
                                    ","
                                )
                                }"
                            )
                        )
                    )
                }
            }
        )
    }
}
