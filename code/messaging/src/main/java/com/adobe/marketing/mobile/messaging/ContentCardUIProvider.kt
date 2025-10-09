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
import com.adobe.marketing.mobile.messaging.ContentCardSchemaDataUtils.SELF_TAG
import com.adobe.marketing.mobile.messaging.ContentCardSchemaDataUtils.buildTemplate
import com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ContentCardUiProvider is responsible for fetching and managing the content for a given surface.
 * It uses Adobe Messaging APIs to retrieve propositions and transform them into UI templates for display.
 *
 * @property surface The surface for which content needs to be fetched.
 */
class ContentCardUIProvider(val surface: Surface) : AepUIContentProvider {
    companion object {
        private const val SELF_TAG: String = "ContentCardUIProvider"
    }

    private val _contentFlow = MutableStateFlow<Result<List<AepUITemplate>>>(Result.success(emptyList()))
    private val contentFlow: StateFlow<Result<List<AepUITemplate>>> = _contentFlow.asStateFlow()

    private val aepUiFlow: Flow<Result<List<AepUI<*, *>>>> = contentFlow.map { templateResult ->
        if (templateResult.isSuccess) {
            Result.success(
                templateResult.getOrDefault(emptyList())
                    .mapNotNull { item -> ContentCardSchemaDataUtils.getAepUI(item) }
            )
        } else {
            Result.failure(
                templateResult.exceptionOrNull()
                    ?: Throwable("Failed to process propositions for surface ${surface.uri}: Unknown Error")
            )
        }
    }

    /**
     * Retrieves a flow of AepUI instances for the given surface.
     *
     * This function initiates the content fetch using [getContent] and then returns a flow of
     * [AepUI] instances that represent the UI templates. The flow emits updates whenever new
     * content is fetched or any changes occur.
     *
     * @return A [Flow] that emits a [Result] containing a list of [AepUI] instances.
     */
    @Deprecated("Use getContentCardUIFlow instead", ReplaceWith("getContentCardUIFlow"))
    suspend fun getContentCardUI(): Flow<Result<List<AepUI<*, *>>>> {
        getContent()
        return aepUiFlow
    }

    /**
     * Refreshes the content cards by fetching new propositions from the surface and updating
     * the flow returned by [getContentCardUIFlow]. This will cause all collectors of the flow
     * to receive the updated content.
     *
     * Note: [getContentCardUIFlow] automatically loads initial content when first collected,
     * so this method is only needed for manual refresh operations.
     */
    override suspend fun refreshContent() {
        getContent()
    }

    /**
     * Initiates fetching of [AepUITemplate] instances for the given surface and returns a flow that emits updates.
     *
     * This function fetches new content by invoking [getAepUITemplateList], which retrieves
     * propositions and builds a list of [AepUITemplate]. The result is posted to the [_contentFlow],
     * which is returned as a [Flow].
     *
     * @return A [Flow] that emits a [Result] containing lists of [AepUITemplate] whenever new content is available.
     */
    @Deprecated("Use getContentCardUIFlow instead", ReplaceWith("getContentCardUIFlow"))
    override suspend fun getContent(): Flow<Result<List<AepUITemplate>>> {
        val propositionsResult = getPropositionsForSurface()
        val templateResult = getAepUITemplateList(propositionsResult)
        _contentFlow.update { templateResult }
        return contentFlow
    }

    /**
     * Retrieves a reactive flow of AepUI instances for the given surface.
     *
     * This function returns a [Flow] that emits a Result of [AepUI] instances.
     * The flow automatically loads initial content when first collected, then continues
     * to emit updates whenever [refreshContent] is called.
     *
     * All collectors will automatically receive the loaded content and any future updates.
     *
     * @return A [Flow] that emits a [Result] containing a list of [AepUI] instances.
     */
    override fun getContentCardUIFlow(): Flow<Result<List<AepUI<*, *>>>> =
        aepUiFlow.onStart {
            // Ensure initial content is loaded when flow is first collected
            getContent()
        }

    /**
     * Converts the provided Result<Map<Surface, List<Proposition>> into a result of list of [AepUITemplate]
     * for the specified surface.
     *
     * If the input result is successful and the proposition can be built into a template,
     * a [Result.success] containing the list of templates is returned.
     *
     * If any proposition fails to be built into a template, an error is logged and the proposition
     * is skipped in the resulting list.
     *
     * If no propositions are found for the surface, or if the input result is a failure,
     * a [Result.Failure] is returned.
     *
     * @param propositionsResult The result containing a map of the requested surface and its propositions.
     * @return A [Result] containing a list of [AepUITemplate] or an error if fetching or building fails.
     */
    private fun getAepUITemplateList(propositionsResult: Result<Map<Surface, List<Proposition>>>): Result<List<AepUITemplate>> {
        return if (propositionsResult.isSuccess) {
            val templateModelList = propositionsResult.getOrNull()?.get(surface)?.mapNotNull { proposition ->
                try {
                    buildTemplate(proposition)
                } catch (e: IllegalArgumentException) {
                    Log.error(
                        LOG_TAG,
                        SELF_TAG,
                        "Failed to build template for proposition ID : ${proposition.uniqueId} ${e.message}"
                    )
                    null
                }
            } ?: emptyList()
            Result.success(templateModelList)
        } else {
            Result.failure(
                propositionsResult.exceptionOrNull()
                    ?: Throwable("Failed to retrieve propositions for surface ${surface.uri}: Unknown Error")
            )
        }
    }

    /**
     * Fetches propositions for the current surface by converting the [Messaging.getPropositionsForSurfaces]
     * API into a suspend function.
     */
    private suspend fun getPropositionsForSurface(): Result<Map<Surface, List<Proposition>>> =
        suspendCancellableCoroutine { continuation ->
            val callback = object : AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
                override fun call(resultMap: Map<Surface, List<Proposition>>?) {
                    if (resultMap == null) {
                        continuation.resume(
                            Result.failure(
                                Throwable("No propositions found for surfaces ${surface.uri}")
                            )
                        )
                    } else {
                        continuation.resume(Result.success(resultMap))
                    }
                }

                override fun fail(error: AdobeError?) {
                    continuation.resume(
                        Result.failure(
                            Throwable(
                                "Failed to retrieve propositions for surface ${surface.uri} " +
                                    "Adobe Error : ${error?.errorName}"
                            )
                        )
                    )
                }
            }

            Messaging.getPropositionsForSurfaces(listOf(surface), callback)
            continuation.invokeOnCancellation {
                Log.debug(LOG_TAG, SELF_TAG, "getPropositionsForSurface connection cancelled")
            }
        }
}
