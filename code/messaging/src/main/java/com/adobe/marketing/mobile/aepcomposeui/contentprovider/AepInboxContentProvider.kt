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

package com.adobe.marketing.mobile.aepcomposeui.contentprovider

import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import kotlinx.coroutines.flow.Flow

/**
 * Responsible for retrieving and refreshing data as required by the inbox UI
 * Classes implementing this interface will define a strategy to provide content for rendering the inbox UI.
 */
interface AepInboxContentProvider {
    /**
     * Retrieves the Inbox content and updates the state as a flow.
     * @return The content for the Inbox as a flow of [InboxUIState].
     */
    fun getInboxUI(): Flow<InboxUIState>

    /**
     * Refreshes the Inbox content and updates the data into the flow returned by [getInboxUI].
     */
    suspend fun refresh()
}
