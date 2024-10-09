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

package com.adobe.marketing.mobile.aepcomposeui.observers

import com.adobe.marketing.mobile.aepcomposeui.interactions.UIEvent

/**
 * Interface for observing events related to AEP UI components.
 *
 * This interface defines a mechanism for handling various types of events triggered by lifecycle changes or user interactions
 * with UI elements, such as display, dismiss, or user interaction events.
 */
interface AepUIEventObserver {

    /**
     * Called when an event related to a UI template occurs.
     *
     * @param event The event to handle. Implementers can provide specific logic based on the type of [UIEvent],
     * which may represent lifecycle event such as display, dismiss, or interaction events.
     */
    fun onEvent(event: UIEvent<*, *>)
}
