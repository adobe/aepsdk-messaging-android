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

package com.adobe.marketing.mobile.aepcomposeui.aepui.state

/**
 * Class representing the state of an AEP card.
 *
 * This class includes the common properties `dismissed`, `selected`, and `read` which are common across different card states.
 *
 * @property dismissed Indicates whether the card has been dismissed.
 * @property selected Indicates whether the card is selected.
 * @property read Indicates whether the card has been read.
 */
open class AepCardUIState(
    open val dismissed: Boolean = false,
    open val selected: Boolean = false,
    open val read: Boolean = false
)
