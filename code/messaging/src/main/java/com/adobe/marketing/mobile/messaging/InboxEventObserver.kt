/*
  Copyright 2026 Adobe. All rights reserved.
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

/**
 * Messaging implementation of [AepInboxEventObserver] for use with [AepInbox].
 * Handles inbox-level events and Adobe Journey Optimizer tracking inside the SDK, and forwards
 * item-level [UIEvent] instances to [itemEventObserver] or to a default [ContentCardEventObserver] when null.
 *
 * @param provider The [MessagingInboxProvider] that owns inbox state
 * @param itemEventObserver Optional [AepUIEventObserver] for item-level events (e.g. [ContentCardEventObserver]).
 *   When null, a default [ContentCardEventObserver] with null callback is used.
 */
class InboxEventObserver(
    private val provider: MessagingInboxProvider,
    private val itemEventObserver: AepUIEventObserver? = null
) : AepInboxEventObserver {

    private val trackingObserver by lazy {
        object : AepInboxEventObserver {
            override fun onInboxEvent(event: InboxEvent) {
                when (event) {
                    is InboxEvent.Display -> {
                        if (!event.inboxUIState.displayed) {
                            ContentCardMapper.instance.getInboxPropositionItem(event.inboxUIState.template.id)
                                ?.track(MessagingEdgeEventType.DISPLAY)
                        }
                    }
                }
            }

            override fun onEvent(event: UIEvent<*, *>) {
                // Currently no-op for item events
            }
        }
    }

    private val inboxObservers by lazy {
        listOf(provider.inboxEventObserver, trackingObserver)
    }

    private val itemObserver: AepUIEventObserver by lazy {
        itemEventObserver ?: ContentCardEventObserver(null)
    }

    override fun onInboxEvent(event: InboxEvent) {
        inboxObservers.forEach { it.onInboxEvent(event) }
    }

    override fun onEvent(event: UIEvent<*, *>) {
        itemObserver.onEvent(event)
        provider.inboxEventObserver.onEvent(event)
    }
}
