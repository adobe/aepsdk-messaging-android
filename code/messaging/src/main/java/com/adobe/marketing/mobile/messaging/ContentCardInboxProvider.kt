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
import androidx.core.graphics.toColorInt
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepInboxContentProvider
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ContentCardInboxProvider is responsible for fetching the Inbox content and
 * manage the Inbox state through reactive updates when the content needs to be refreshed.
 *
 * @param surface The surface to fetch content cards for.
 */
class ContentCardInboxProvider(val surface: Surface) : AepInboxContentProvider {
    companion object {
        private const val SELF_TAG: String = "ContentCardInboxProvider"
    }

    // Internal state flow for refresh() to update
    private val _inboxStateFlow = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    private val inboxStateFlow = _inboxStateFlow.asStateFlow()

    private fun toInboxUIState(result: Result<Pair<Proposition, List<Proposition>>>): InboxUIState {
        val propositions = result.getOrNull()
        if (propositions != null) {
            val inboxTemplate = createInboxTemplate(propositions.first)
            val aepUIList = propositions.second.mapNotNull { cardProposition ->
                ContentCardSchemaDataUtils.buildTemplate(cardProposition)?.let { template ->
                    ContentCardSchemaDataUtils.getAepUI(
                        template,
                        false // todo: replace with actual read/unread in subsequent PR
                    )
                }
            }
            return InboxUIState.Success(
                inboxTemplate,
                items = aepUIList
            )
        } else {
            return InboxUIState.Error(
                result.exceptionOrNull()
                    ?: Throwable("Failed to create inbox: Unknown Error")
            )
        }
    }

    /**
     * Retrieves the Inbox content and updates the state as a flow.
     * @return The content for the Inbox as a flow of [InboxUIState].
     */
    override fun getInboxUI(): Flow<InboxUIState> = flow {
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
        _inboxStateFlow.value = toInboxUIState(fetchContent())
    }

    /**
     * Fetches the inbox and content card propositions by calling [Messaging.getPropositionsForSurfaces]
     *
     * @return A [Pair] containing a [Result] with the [InboxTemplate] and a list of [Proposition]s.
     */
    private suspend fun fetchContent(): Result<Pair<Proposition, List<Proposition>>> {
        return try {
            // Call Messaging API to get propositions for this surface
            val propositionsMap = getPropositionsForSurface()

            // Get propositions for our surface
            val propositions = propositionsMap[surface]
            if (propositions.isNullOrEmpty()) {
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "No propositions found for surface: ${surface.uri}"
                )
                return Result.failure(Throwable("No propositions found for surface"))
            }

            return Result.success(Pair(getMockInboxProposition(), propositions))
        } catch (e: Exception) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Error fetching container: ${e.message}"
            )
            Result.failure(e)
        }
    }

    /**
     * Calls Messaging.getPropositionsForSurfaces and converts it to a suspend function.
     */
    private suspend fun getPropositionsForSurface(): Map<Surface, List<Proposition>> =
        suspendCancellableCoroutine { continuation ->
            val callback = object : AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
                override fun call(resultMap: Map<Surface, List<Proposition>>?) {
                    if (resultMap == null) {
                        continuation.resume(emptyMap())
                    } else {
                        continuation.resume(resultMap)
                    }
                }

                override fun fail(error: AdobeError?) {
                    Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Failed to get propositions for surface ${surface.uri}: ${error?.errorName}"
                    )
                    continuation.resume(emptyMap())
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
     * Creates the appropriate inbox template based on inbox proposition.
     */
    // todo: replace with constants once JSON is finalized
    private fun createInboxTemplate(inboxProposition: Proposition): InboxTemplate {
        val data = inboxProposition.items[0].itemData
        val heading = (data["heading"] as? Map<*, *>)?.get("content") as? String
        val layout = (data["layout"] as? Map<*, *>)?.get("orientation") as? String
        val capacity = when (val capacityObj = data["capacity"]) {
            is Number -> capacityObj.toInt()
            else -> 10 // Default capacity
        }
        val emptyStateMessage = (data["emptyStateSettings"] as? Map<*, *>)
            ?.let { it["message"] as? Map<*, *> }
            ?.get("content") as? String
        val emptyImage = (data["emptyStateSettings"] as? Map<*, *>)
            ?.let { it["image"] as? Map<*, *> }
        val isUnreadEnabled = data["isUnreadEnabled"] as? Boolean ?: false
        val unreadIcon = (data["unread_indicator"] as? Map<*, *>)
            ?.let { it["unread_icon"] as? Map<*, *> }
            ?.let { it["image"] as? Map<*, *> }
        val unreadBg = (data["unread_indicator"] as? Map<*, *>)
            ?.let { it["unread_bg"] as? Map<*, *> }

        return InboxTemplate(
            heading = heading?.let { AepText(it) } ?: AepText("Message Inbox"),
            layout = AepInboxLayout.from(layout) ?: AepInboxLayout.VERTICAL,
            capacity = capacity,
            emptyMessage = emptyStateMessage?.let { AepText(it) },
            emptyImage = AepImage(
                url = emptyImage?.get("url") as? String,
                darkUrl = emptyImage?.get("darkUrl") as? String
            ),
            isUnreadEnabled = isUnreadEnabled,
            unreadIcon = AepImage(
                url = unreadIcon?.get("url") as? String,
                darkUrl = unreadIcon?.get("darkUrl") as? String
            ),
            unreadBgColor = AepColor(
                lightColor = (unreadBg?.get("bgColor") as? String)?.let { Color(it.toColorInt()) },
                darkColor = (unreadBg?.get("darkBgColor") as? String)?.let { Color(it.toColorInt()) }
            ),
            unreadIconAlignment = (unreadIcon?.get("placeholder") as? String)?.let { placeholder ->
                when (placeholder.lowercase()) {
                    "topleft" -> Alignment.TopStart
                    "topright" -> Alignment.TopEnd
                    "bottomleft" -> Alignment.BottomStart
                    "bottomright" -> Alignment.BottomEnd
                    else -> Alignment.TopStart
                }
            }
        )
    }

    // todo: remove this mock function when Messaging SDK supports inbox propositions
    private fun getMockInboxProposition(): Proposition {
        return Proposition(
            "inbox_container",
            surface.uri,
            mapOf("key" to "value"),
            listOf(
                PropositionItem(
                    "inbox_template",
                    SchemaType.INBOX,
                    mapOf(
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
                        "unread_indicator" to mapOf(
                            "unread_icon" to mapOf(
                                "placeholder" to "topleft",
                                "image" to mapOf(
                                    "url" to "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png",
                                    "darkUrl" to "https://icons.veryicon.com/png/o/leisure/crisp-app-icon-library-v3/notification-5.png"
                                )
                            ),
                            "unread_bg" to mapOf(
                                "bgColor" to "#A9A9A9",
                                "darkBgColor" to "#D3D3D3"
                            )
                        )
                    )
                )
            )
        )
    }
}
