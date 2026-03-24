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

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepInboxContentProvider
import com.adobe.marketing.mobile.aepcomposeui.observers.AepInboxEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * MessagingInboxProvider is responsible for fetching the Inbox content and
 * manage the Inbox state through reactive updates when the content needs to be refreshed.
 *
 * @param surface The surface to fetch content cards for.
 */
class MessagingInboxProvider(
    val surface: Surface
) : AepInboxContentProvider {
    data class InboxProposition(
        val inbox: Proposition,
        val contentCards: List<Proposition>
    )

    companion object {
        private const val SELF_TAG: String = "MessagingInboxProvider"
    }

    // Internal state flow for refresh() to update
    private val _inboxStateFlow = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    private val inboxStateFlow = _inboxStateFlow.asStateFlow()

    private fun toInboxUIState(result: Result<InboxProposition>): InboxUIState {
        val inboxProposition = result.getOrNull()
        if (inboxProposition != null) {
            val inboxTemplate =
                ContentCardSchemaDataUtils.createInboxTemplate(inboxProposition.inbox)
                    ?: return InboxUIState.Error(Throwable("Failed to create inbox template, invalid inbox proposition content"))
            val aepUIList = inboxProposition.contentCards.mapNotNull { cardProposition ->
                ContentCardSchemaDataUtils.buildTemplate(cardProposition)?.let { template ->
                    ContentCardSchemaDataUtils.getAepUI(
                        template,
                        if (inboxTemplate.isUnreadEnabled) ContentCardSchemaDataUtils.getReadStatus(
                            template.id
                        ) else null
                    )
                }
            }
            // Store the inbox proposition item in the mapper for display tracking, keyed by inbox ID
            inboxProposition.inbox.items.getOrNull(0)?.let {
                ContentCardMapper.instance.storeInboxPropositionItem(inboxTemplate.id, it)
            }
            return InboxUIState.Success(
                template = inboxTemplate,
                items = aepUIList,
                displayed = false
            )
        } else {
            return InboxUIState.Error(
                result.exceptionOrNull()
                    ?: Throwable("Failed to create inbox, unknown error")
            )
        }
    }

    /**
     * Retrieves the Inbox content and updates the state as a flow.
     * @return The content for the Inbox as a flow of [InboxUIState].
     */
    override fun getInboxUI(): Flow<InboxUIState> = flow {
        emit(InboxUIState.Loading)
        refresh()
        emitAll(inboxStateFlow)
    }

    /**
     * Refreshes the Inbox content by fetching new inbox and content cards propositions and
     * updating the flow returned by [getInboxUI]. This will cause all collectors of the flow
     * to receive the updated inbox.
     *
     * Emits [InboxUIState.Loading] before fetching, then emits [InboxUIState.Success] or [InboxUIState.Error].
     *
     * Note: [getInboxUI] automatically loads initial content when first collected,
     * so this method is only needed for manual refresh operations.
     */
    override suspend fun refresh() {
        _inboxStateFlow.update { InboxUIState.Loading }
        _inboxStateFlow.update { toInboxUIState(fetchInbox()) }
    }

    /**
     * Fetches the inbox and content card propositions by calling [Messaging.getPropositionsForSurfaces]
     *
     * @return A [Result] containing the [InboxProposition] on success, or a failure with the error.
     */
    private suspend fun fetchInbox(): Result<InboxProposition> {
        val propositionsResult = getPropositionsForSurface()

        if (propositionsResult.isFailure) {
            return Result.failure(
                propositionsResult.exceptionOrNull()
                    ?: Throwable("Failed to fetch propositions for surface: ${surface.uri}")
            )
        }

        val propositionsMap = propositionsResult.getOrNull() ?: emptyMap()

        val inboxPropositionList =
            propositionsMap[surface]?.filter { it.items.isNotEmpty() && it.items[0].schema == SchemaType.INBOX }
        if (inboxPropositionList.isNullOrEmpty()) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "No inbox proposition found for surface: ${surface.uri}"
            )
            return Result.failure(Throwable("No inbox proposition found for surface: ${surface.uri}"))
        }
        // IDS response has the propositions ordered by priority and last modified time, so the first proposition with the inbox schema should be the one to display
        val highestPriorityInbox = inboxPropositionList.first()
        val contentCardPropositions =
            propositionsMap[surface]?.filter { it.items.isNotEmpty() && it.items[0].schema == SchemaType.CONTENT_CARD }
                ?: emptyList()
        return Result.success(InboxProposition(highestPriorityInbox, contentCardPropositions))
    }

    /**
     * Calls Messaging.getPropositionsForSurfaces and converts it to a suspend function.
     *
     * @return A [Result] containing the propositions map on success, or a failure with the error.
     */
    private suspend fun getPropositionsForSurface(): Result<Map<Surface, List<Proposition>>> =
        suspendCancellableCoroutine { continuation ->
            val callback = object : AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
                override fun call(resultMap: Map<Surface, List<Proposition>>?) {
                    if (resultMap.isNullOrEmpty()) {
                        continuation.resume(
                            Result.failure(
                                Throwable("Received null propositions map for surface: ${surface.uri}")
                            )
                        )
                    } else {
                        continuation.resume(Result.success(resultMap))
                    }
                }

                override fun fail(error: AdobeError?) {
                    continuation.resume(
                        Result.failure(
                            Throwable("Failed to get propositions: ${error?.errorName ?: "Unknown error"}")
                        )
                    )
                }
            }

            Messaging.getPropositionsForSurfaces(listOf(surface), callback)
            continuation.invokeOnCancellation {
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "getPropositionsForSurface cancelled"
                )
            }
        }

    /**
     * Internal observer that handles state updates needed for inbox events.
     * Kept internal to prevent integrating apps from calling these methods directly.
     */
    internal val inboxEventObserver: AepInboxEventObserver = object : AepInboxEventObserver {
        override fun onInboxEvent(event: InboxEvent) {
            when (event) {
                is InboxEvent.Display -> {
                    _inboxStateFlow.update { event.inboxUIState.copy(displayed = true) }
                }
            }
        }

        override fun onEvent(event: UIEvent<*, *>) {
            if (event is UIEvent.Dismiss) {
                val currentState = _inboxStateFlow.value
                if (currentState is InboxUIState.Success) {
                    val dismissedId = event.aepUi.getTemplate().id
                    _inboxStateFlow.update {
                        currentState.copy(
                            items = currentState.items.filter {
                                it.getTemplate().id != dismissedId
                            }
                        )
                    }
                }
            }
        }
    }
}
