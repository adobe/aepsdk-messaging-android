/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class InternalMessagingUtilsTests {
    private final String mockJsonObj = "{\n" +
            "   \"messageProfile\":{\n" +
            "      \"channel\":{\n" +
            "         \"_id\":\"https://ns.adobe.com/xdm/channels/push\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"pushChannelContext\":{\n" +
            "      \"platform\":\"fcm\"\n" +
            "   }\n" +
            "}";
    private final String mockJsonArr = "[\n" +
            "   {\n" +
            "      \"channel\": {\n" +
            "         \"_id\": \"https://ns.adobe.com/xdm/channels/push\"\n" +
            "      }\n" +
            "   },\n" +
            "   {\n" +
            "      \"platform\": \"fcm\"\n" +
            "   }\n" +
            "]";

    // ========================================================================================
    // toMap
    // ========================================================================================
    @Test
    public void test_toMap() {
        try {
            // mock
            JSONObject json = new JSONObject(mockJsonObj);

            // test
            Map<String, Object> result = JSONUtils.toMap(json);

            if (result == null) {
                Assert.fail();
            }

            // verify
            Assert.assertTrue(result.containsKey("messageProfile"));
            Assert.assertTrue(result.containsKey("pushChannelContext"));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void test_toMap_when_nullJson() {
        try {
            Assert.assertNull(JSONUtils.toMap(null));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    // ========================================================================================
    // toList
    // ========================================================================================
    @Test
    public void test_toList() {
        try {
            // mock
            JSONArray json = new JSONArray(mockJsonArr);

            // test
            List<Object> result = JSONUtils.toList(json);

            // verify
            Assert.assertEquals(result.size(), 2);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void test_toList_when_nullJson() {
        try {
            Assert.assertNull(JSONUtils.toList(null));
        } catch (JSONException e) {
            Assert.fail();
        }
    }
}
