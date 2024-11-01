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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UIActionTests {

    @Test
    fun `Create a Click UIAction`() {
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        assertEquals("button1", action.id)
        assertEquals("http://example.com", action.actionUrl)
    }

    @Test
    fun `Create a Click UIAction without a url`() {
        val action = UIAction.Click(id = "button1", actionUrl = null)
        assertEquals("button1", action.id)
        assertNull(action.actionUrl)
    }
}
