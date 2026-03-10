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

import com.adobe.marketing.mobile.aepcomposeui.InboxEvent
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepInboxEventObserver
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver

/**
 * Messaging implementation of [AepInboxEventObserver] for use as the [AepInbox] `observer` parameter.
 * Routes inbox-level events to tracking ([InboxEventObserver]) and state update ([MessagingInboxProvider.onInboxEvent]),
 * and forwards item-level events to the provided [AepUIEventObserver] or a default [ContentCardEventObserver] when null.
 *
 * @param provider The [MessagingInboxProvider] that owns inbox state; the observer calls
 *   [MessagingInboxProvider.onInboxEvent] so the provider updates its state.
 * @param itemEventObserver Optional [AepUIEventObserver] for item-level events (e.g. [ContentCardEventObserver]).
 *   When null, a default [ContentCardEventObserver] with null callback is used.
 */
class MessagingInboxEventObserver(
    private val provider: MessagingInboxProvider,
    private val itemEventObserver: AepUIEventObserver? = null
) : AepInboxEventObserver {

    private val trackingObserver = InboxEventObserver()

    private val observer: AepUIEventObserver by lazy {
        itemEventObserver ?: ContentCardEventObserver(null)
    }

    override fun onInboxEvent(event: InboxEvent) {
        trackingObserver.onInboxEvent(event)
        provider.onInboxEvent(event)
    }

    override fun onEvent(event: UIEvent<*, *>) {
        observer.onEvent(event)
    }
}
