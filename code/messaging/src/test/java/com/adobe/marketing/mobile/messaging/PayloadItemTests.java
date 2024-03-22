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

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PayloadItemTests {
    Map<String, Object> dataMap =
            new HashMap<String, Object>() {
                {
                    put("id", "dataid");
                    put("content", "content");
                }
            };
    Map<String, Object> payloadMap =
            new HashMap<String, Object>() {
                {
                    put("id", "id");
                    put("schema", "schema");
                    put("data", dataMap);
                }
            };

    private PayloadItem payloadItem;

    @Test
    public void testPayloadItem_ValidPayload() {
        // test
        try {
            payloadItem = new PayloadItem(payloadMap);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // verify
        assertNotNull(payloadItem);
        assertEquals("id", payloadItem.id);
        assertEquals("schema", payloadItem.schema);
        assertEquals("dataid", payloadItem.data.get("id"));
        assertEquals("content", payloadItem.data.get("content"));
    }

    @Test
    public void testPayloadItem_NullId() {
        // setup
        payloadMap.put("id", null);
        // test
        try {
            payloadItem = new PayloadItem(payloadMap);
        } catch (Exception e) {
        }
        // verify
        assertNull(payloadItem);
    }

    @Test
    public void testPayloadItem_NullSchema() {
        // setup
        payloadMap.put("schema", null);
        // test
        try {
            payloadItem = new PayloadItem(payloadMap);
        } catch (Exception e) {
        }
        // verify
        assertNull(payloadItem);
    }

    @Test
    public void testPayloadItem_NullData() {
        // setup
        payloadMap.put("data", null);
        // test
        try {
            payloadItem = new PayloadItem(payloadMap);
        } catch (Exception e) {
        }
        // verify
        assertNull(payloadItem);
    }
}
