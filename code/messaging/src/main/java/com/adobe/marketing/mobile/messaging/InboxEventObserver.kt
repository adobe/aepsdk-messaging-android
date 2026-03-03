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

import com.adobe.marketing.mobile.MessagingEdgeEventType
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepInboxEventObserver
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState

/**
 * Implementation of [AepInboxEventObserver] for handling inbox-level events.
 *
 * This observer handles inbox events (such as display tracking) and delegates individual
 * item-level events to the provided [AepUIEventObserver] instances. This design allows the
 * inbox to support additional message types (e.g., JSON, HTML) in the future by simply
 * adding new observers — without modifying this class.
 *
 * @param itemEventObservers Zero or more [AepUIEventObserver] instances that handle
 *   item-level events (e.g., [ContentCardEventObserver]). Each observer's [onEvent] is
 *   called for every item-level [UIEvent]. If no observers are provided, a default
 *   [ContentCardEventObserver] with null callback will be used.
 */
class InboxEventObserver(
    private vararg val itemEventObservers: AepUIEventObserver
) : AepInboxEventObserver {

    private var updateInboxState: ((InboxUIState) -> Unit)? = null

    /**
     * Sets the callback to update the inbox state. This should be called by the content provider
     * that owns this observer to establish the connection between the observer and the state flow.
     *
     * Note: Each InboxEventObserver should be used with only one MessagingInboxProvider.
     * Sharing an observer instance across multiple providers is not supported and will result
     * in an IllegalStateException.
     *
     * @param callback The callback to invoke when the inbox state needs to be updated.
     * @throws IllegalStateException if a callback has already been set (observer is being reused).
     */
    internal fun setInboxStateUpdateCallback(callback: (InboxUIState) -> Unit) {
        require(updateInboxState == null) {
            "InboxEventObserver callback already set. Each observer instance should only be used with one MessagingInboxProvider."
        }
        updateInboxState = callback
    }

    private val observers: List<AepUIEventObserver> by lazy {
        if (itemEventObservers.isEmpty()) {
            listOf(ContentCardEventObserver(null))
        } else {
            itemEventObservers.toList()
        }
    }

    /**
     * Delegates item-level events to all provided [AepUIEventObserver] instances.
     * If no observers were provided, delegates to a default [ContentCardEventObserver].
     */
    override fun onEvent(event: UIEvent<*, *>) {
        observers.forEach { it.onEvent(event) }
    }

    /**
     * Handles inbox-level events.
     * Currently handles [InboxEvent.Display] by tracking the inbox proposition.
     *
     * @param event The [InboxEvent] to handle.
     */
    override fun onInboxEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.Display -> {
                ContentCardMapper.instance.getInboxPropositionItem(event.inboxUIState.template.id)
                    ?.track(MessagingEdgeEventType.DISPLAY)
                updateInboxState?.invoke(event.inboxUIState.copy(displayed = true))
            }
        }
    }
}
