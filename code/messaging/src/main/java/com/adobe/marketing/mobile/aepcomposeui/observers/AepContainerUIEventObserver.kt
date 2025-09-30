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

package com.adobe.marketing.mobile.aepcomposeui.observers

import com.adobe.marketing.mobile.aepcomposeui.ContainerEvent
import com.adobe.marketing.mobile.messaging.ContentCardUIEventListener

/**
 * Interface for observing events related to AEP container UI and Aep UIs inside the container.
 */
interface AepContainerUIEventObserver : ContentCardUIEventListener {
    abstract val aepUIEventObserver: AepUIEventObserver
    /**
     * Called when an event related to a container UI occurs.
     *
     * @param event The container event to handle. Implementers can provide specific logic based on the type of [ContainerEvent],
     * which may represent lifecycle events such as display, dismiss, or interaction events.
     */
    fun onContainerEvent(event: ContainerEvent<*, *>)
}
