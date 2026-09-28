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

import com.adobe.marketing.mobile.messaging.MessagingPushConstants.PushInteractionType
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the string contract shared with the UI template add-on (aepsdk-ui-android) through Core's
 * IUiTemplatePlugin. Neither SDK depends on the other, so the values are duplicated. The add-on has an
 * identical fixture in its own PluginContractTest; changing a value here requires the same change there.
 */
class PluginContractTest {

    // Keep identical to the fixture in aepsdk-ui-android PluginContractTest.
    private val expectedInteractionTypes = mapOf(
        "CONTENT_CLICK" to "content_click",
        "BUTTON_CLICK" to "button_click",
        "DISMISS" to "dismiss",
        "INPUT_SUBMIT" to "input_submit",
        "RERENDER" to "rerender"
    )

    // Keep identical to the fixture in aepsdk-ui-android PluginContractTest.
    private val expectedReservedKeys = mapOf(
        "MESSAGE_ID" to "messageId",
        "NOTIFICATION_ID" to "notificationId"
    )

    @Test
    fun `interaction type strings match the shared contract`() {
        assertEquals(
            expectedInteractionTypes,
            mapOf(
                "CONTENT_CLICK" to PushInteractionType.CONTENT_CLICK,
                "BUTTON_CLICK" to PushInteractionType.BUTTON_CLICK,
                "DISMISS" to PushInteractionType.DISMISS,
                "INPUT_SUBMIT" to PushInteractionType.INPUT_SUBMIT,
                "RERENDER" to PushInteractionType.RERENDER
            )
        )
    }

    @Test
    fun `reserved message-data keys match the shared contract`() {
        assertEquals(
            expectedReservedKeys,
            mapOf(
                "MESSAGE_ID" to Tracking.Keys.MESSAGE_ID,
                "NOTIFICATION_ID" to Tracking.Keys.NOTIFICATION_ID
            )
        )
    }
}
