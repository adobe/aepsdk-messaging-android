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

package com.adobe.marketing.mobile.aepcomposeui

/**
 * Represents an action that can be performed on a UI component.
 */
sealed class UIAction {
    /**
     * Represents a click action that can be performed on a UI component
     * @property id unique identifier of the UI component
     * @property actionUrl optional URL to be opened when the UI component is clicked
     */
    data class Click(val id: String, val actionUrl: String?) : UIAction()
}
