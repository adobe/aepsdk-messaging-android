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

package com.adobe.marketing.mobile.aepcomposeui.uimodels

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxTemplateTests {

    @Test
    fun `create InboxTemplate with required parameters only`() {
        val heading = AepText("Inbox")
        val layout = AepInboxLayout.VERTICAL
        val capacity = 10

        val template = InboxTemplate(
            heading = heading,
            layout = layout,
            capacity = capacity
        )

        assertEquals(heading, template.heading)
        assertEquals(layout, template.layout)
        assertEquals(capacity, template.capacity)
        assertNull(template.emptyMessage)
        assertNull(template.emptyImage)
        assertFalse(template.isUnreadEnabled)
        assertNull(template.unreadBgColor)
        assertNull(template.unreadIcon)
        assertNull(template.unreadIconAlignment)
    }

    @Test
    fun `create InboxTemplate with all parameters`() {
        val heading = AepText("My Inbox")
        val layout = AepInboxLayout.HORIZONTAL
        val capacity = 20
        val emptyMessage = AepText("No messages")
        val emptyImage = AepImage(url = "https://example.com/empty.png")
        val isUnreadEnabled = true
        val unreadBgColor = AepColor(light = Color.Red, dark = Color.DarkGray)
        val unreadIcon = AepImage(url = "https://example.com/unread.png")
        val unreadIconAlignment = Alignment.TopEnd

        val template = InboxTemplate(
            heading = heading,
            layout = layout,
            capacity = capacity,
            emptyMessage = emptyMessage,
            emptyImage = emptyImage,
            isUnreadEnabled = isUnreadEnabled,
            unreadBgColor = unreadBgColor,
            unreadIcon = unreadIcon,
            unreadIconAlignment = unreadIconAlignment
        )

        assertEquals(heading, template.heading)
        assertEquals(layout, template.layout)
        assertEquals(capacity, template.capacity)
        assertEquals(emptyMessage, template.emptyMessage)
        assertEquals(emptyImage, template.emptyImage)
        assertTrue(template.isUnreadEnabled)
        assertEquals(unreadBgColor, template.unreadBgColor)
        assertEquals(unreadIcon, template.unreadIcon)
        assertEquals(unreadIconAlignment, template.unreadIconAlignment)
    }

    @Test
    fun `create InboxTemplate with vertical layout`() {
        val template = InboxTemplate(
            heading = AepText("Vertical Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 5
        )

        assertEquals(AepInboxLayout.VERTICAL, template.layout)
        assertEquals("vertical", template.layout.typeName)
    }

    @Test
    fun `create InboxTemplate with horizontal layout`() {
        val template = InboxTemplate(
            heading = AepText("Horizontal Inbox"),
            layout = AepInboxLayout.HORIZONTAL,
            capacity = 5
        )

        assertEquals(AepInboxLayout.HORIZONTAL, template.layout)
        assertEquals("horizontal", template.layout.typeName)
    }

    @Test
    fun `create InboxTemplate with empty message and image`() {
        val emptyMessage = AepText("Your inbox is empty")
        val emptyImage = AepImage(
            url = "https://example.com/empty-light.png",
            darkUrl = "https://example.com/empty-dark.png"
        )

        val template = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            emptyMessage = emptyMessage,
            emptyImage = emptyImage
        )

        assertEquals(emptyMessage, template.emptyMessage)
        assertEquals("Your inbox is empty", template.emptyMessage?.content)
        assertEquals(emptyImage, template.emptyImage)
        assertEquals("https://example.com/empty-light.png", template.emptyImage?.url)
        assertEquals("https://example.com/empty-dark.png", template.emptyImage?.darkUrl)
    }

    @Test
    fun `create InboxTemplate with unread indicator enabled`() {
        val unreadBgColor = AepColor(light = Color.Blue, dark = Color.Cyan)
        val unreadIcon = AepImage(url = "https://example.com/unread-icon.png")
        val unreadIconAlignment = Alignment.BottomStart

        val template = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            isUnreadEnabled = true,
            unreadBgColor = unreadBgColor,
            unreadIcon = unreadIcon,
            unreadIconAlignment = unreadIconAlignment
        )

        assertTrue(template.isUnreadEnabled)
        assertEquals(unreadBgColor, template.unreadBgColor)
        assertEquals(unreadIcon, template.unreadIcon)
        assertEquals(unreadIconAlignment, template.unreadIconAlignment)
    }

    @Test
    fun `create InboxTemplate with unread indicator disabled`() {
        val template = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10,
            isUnreadEnabled = false
        )

        assertFalse(template.isUnreadEnabled)
    }

    @Test
    fun `InboxTemplate data class equality`() {
        val template1 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )
        val template2 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )

        assertEquals(template1, template2)
    }

    @Test
    fun `InboxTemplate data class inequality with different heading`() {
        val template1 = InboxTemplate(
            heading = AepText("Inbox 1"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )
        val template2 = InboxTemplate(
            heading = AepText("Inbox 2"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )

        assertFalse(template1 == template2)
    }

    @Test
    fun `InboxTemplate data class inequality with different layout`() {
        val template1 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )
        val template2 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.HORIZONTAL,
            capacity = 10
        )

        assertFalse(template1 == template2)
    }

    @Test
    fun `InboxTemplate data class inequality with different capacity`() {
        val template1 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )
        val template2 = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 20
        )

        assertFalse(template1 == template2)
    }

    @Test
    fun `InboxTemplate copy with modified heading`() {
        val original = InboxTemplate(
            heading = AepText("Original"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )

        val modified = original.copy(heading = AepText("Modified"))

        assertEquals("Modified", modified.heading.content)
        assertEquals(original.layout, modified.layout)
        assertEquals(original.capacity, modified.capacity)
    }

    @Test
    fun `InboxTemplate copy with modified layout`() {
        val original = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 10
        )

        val modified = original.copy(layout = AepInboxLayout.HORIZONTAL)

        assertEquals(original.heading, modified.heading)
        assertEquals(AepInboxLayout.HORIZONTAL, modified.layout)
        assertEquals(original.capacity, modified.capacity)
    }

    @Test
    fun `InboxTemplate with various alignment values`() {
        val alignments = listOf(
            Alignment.TopStart,
            Alignment.TopCenter,
            Alignment.TopEnd,
            Alignment.CenterStart,
            Alignment.Center,
            Alignment.CenterEnd,
            Alignment.BottomStart,
            Alignment.BottomCenter,
            Alignment.BottomEnd
        )

        alignments.forEach { alignment ->
            val template = InboxTemplate(
                heading = AepText("Inbox"),
                layout = AepInboxLayout.VERTICAL,
                capacity = 10,
                unreadIconAlignment = alignment
            )
            assertEquals(alignment, template.unreadIconAlignment)
        }
    }

    @Test
    fun `InboxTemplate with zero capacity`() {
        val template = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = 0
        )

        assertEquals(0, template.capacity)
    }

    @Test
    fun `InboxTemplate with large capacity`() {
        val template = InboxTemplate(
            heading = AepText("Inbox"),
            layout = AepInboxLayout.VERTICAL,
            capacity = Int.MAX_VALUE
        )

        assertEquals(Int.MAX_VALUE, template.capacity)
    }
}
