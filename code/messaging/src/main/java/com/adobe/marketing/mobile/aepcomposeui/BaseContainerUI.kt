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

package com.adobe.marketing.mobile.aepcomposeui

import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.aepcomposeui.state.AepContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepContainerUITemplate

sealed class BaseContainerUI<T : AepContainerUITemplate, S : AepContainerUIState>(
    private val template: T,
    state: S
) : AepContainerUI<T, S> {
    private val _state = mutableStateOf(state)

    override fun getTemplate(): T {
        return template
    }

    override fun getState(): S {
        return _state.value
    }

    override fun updateState(newState: S) {
        _state.value = newState
    }
}
