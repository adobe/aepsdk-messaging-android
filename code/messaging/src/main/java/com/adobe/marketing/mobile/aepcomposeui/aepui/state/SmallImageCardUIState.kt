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
 * Class representing the UI state of a Small Image template card.
 *
 * @property dismissed Indicates whether the card has been dismissed.
 * @property selected Indicates whether the card is selected.
 * @property read Indicates whether the card has been read.
 * @property timerDisplayed Indicates whether the timer has been displayed.
 * @property cardExpanded Indicates whether the card has been expanded.
 */
data class SmallImageCardUIState(
    var timerDisplayed: Boolean = false,
    var cardExpanded: Boolean = false,
    override var dismissed: Boolean = false,
    override var selected: Boolean = false,
    override var read: Boolean = false
) : AepCardUIState()
