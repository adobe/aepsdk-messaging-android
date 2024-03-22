/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ItemDataTests {
    private ItemData itemData;

    @Test
    public void testCreateItemData_ValidPayload() {
        // test
        try {
            itemData = new ItemData("id", "content");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // verify
        assertNotNull(itemData);
        assertEquals("id", itemData.id);
        assertEquals("content", itemData.content);
    }

    @Test
    public void testCreateItemData_MissingId() {
        // test
        try {
            itemData = new ItemData(null, "content");
        } catch (Exception e) {
        }
        // verify
        assertNull(itemData);
    }

    @Test
    public void testCreateItemData_MissingContent() {
        // test
        try {
            itemData = new ItemData("id", null);
        } catch (Exception e) {
        }
        // verify
        assertNull(itemData);
    }
}
