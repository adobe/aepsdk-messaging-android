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
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepInboxContentProvider
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
            val inboxTemplate = ContentCardSchemaDataUtils.createInboxTemplate(inboxProposition.inbox)
            if (inboxTemplate == null) {
                return InboxUIState.Error(Throwable("Failed to create inbox template, invalid inbox proposition content"))
            }
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
     * Updates the inbox state. This is called by [InboxEventObserver] to update
     * the state after handling events (e.g., marking inbox as displayed).
     *
     * @param newState The new [InboxUIState] to emit.
     */
    internal fun updateInboxState(newState: InboxUIState) {
        _inboxStateFlow.value = newState
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
        _inboxStateFlow.value = InboxUIState.Loading
        _inboxStateFlow.value = toInboxUIState(fetchInbox())
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

        // todo replace mock inbox proposition when Messaging SDK supports inbox propositions
        val inboxProposition = getMockInboxProposition()
        if (inboxProposition == null) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "No inbox proposition found for surface: ${surface.uri}"
            )
            return Result.failure(Throwable("No inbox proposition found for surface: ${surface.uri}"))
        }
        val contentCardPropositions =
            propositionsMap[surface]?.filter { it.items.isNotEmpty() && it.items[0].schema == SchemaType.CONTENT_CARD } ?: emptyList()
        return Result.success(InboxProposition(inboxProposition, contentCardPropositions))
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

    // todo: remove this mock function when Messaging SDK supports inbox propositions
    private fun getMockInboxProposition(): Proposition {
        return Proposition(
            "inbox_container",
            surface.uri,
            mapOf("activity" to mapOf("id" to "inbox_container")),
            listOf(
                PropositionItem(
                    "inbox_template",
                    SchemaType.INBOX,
                    mapOf(
                        "content" to mapOf(
                            "heading" to mapOf("content" to "My Inbox"),
                            "layout" to mapOf("orientation" to "vertical"),
                            "capacity" to 10,
                            "emptyStateSettings" to mapOf(
                                "message" to mapOf("content" to "Your inbox is empty!"),
                                "image" to mapOf(
                                    "url" to "https://icons.veryicon.com/png/256/commerce-shopping/basic-icon-of-e-commerce/empty-21.png",
                                    "darkUrl" to "https://icons.veryicon.com/png/256/commerce-shopping/basic-icon-of-e-commerce/empty-21.png"
                                )
                            ),
                            "isUnreadEnabled" to true,
                            "unreadIndicator" to mapOf(
                                "unreadIcon" to mapOf(
                                    "placement" to "topleft",
                                    "image" to mapOf(
                                        "url" to "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png",
                                        "darkUrl" to "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png"
                                    )
                                ),
                                "unreadBg" to mapOf(
                                    "clr" to mapOf(
                                        "light" to "#A9A9A9",
                                        "dark" to "#D3D3D3"
                                    )
                                )
                            )
                        ),
                        "meta" to emptyMap()
                    )
                )
            )
        )
    }
}
