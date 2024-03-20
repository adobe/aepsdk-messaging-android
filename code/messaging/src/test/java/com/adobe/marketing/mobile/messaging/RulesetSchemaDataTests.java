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

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKey.RulesEngine.JSON_RULES_KEY;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKey.RulesEngine.JSON_VERSION_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.services.Log;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RulesetSchemaDataTests {

    @Test
    public void constructor_setsVersionAndRules_whenSchemaDataIsValid() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(JSON_VERSION_KEY, 1);
        Map<String, Object> rule = new HashMap<>();
        rule.put("key", "value");
        schemaData.put(JSON_RULES_KEY, Collections.singletonList(rule));

        // test
        RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

        // verify
        assertEquals(1, rulesetSchemaData.getVersion());
        assertEquals("value", rulesetSchemaData.getRules().get(0).get("key"));
    }

    @Test
    public void constructor_logsException_whenVersionIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            Map<String, Object> rule = new HashMap<>();
            rule.put("key", "value");
            schemaData.put(JSON_RULES_KEY, Collections.singletonList(rule));

            // test
            RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void constructor_logsException_whenRulesAreMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(JSON_VERSION_KEY, 1);

            // test
            RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void constructor_logsException_whenRulesAreNotListOfMaps() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(JSON_VERSION_KEY, 1);
            schemaData.put(JSON_RULES_KEY, "not a list of maps");

            // test
            RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void getContent_returnsRules_whenCalled() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(JSON_VERSION_KEY, 1);
        Map<String, Object> rule = new HashMap<>();
        rule.put("key", "value");
        schemaData.put(JSON_RULES_KEY, Collections.singletonList(rule));
        RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

        // test
        Object content = rulesetSchemaData.getContent();

        // verify
        assertEquals(Collections.singletonList(rule), content);
    }

    @Test
    public void getContent_returnsNull_whenRulesAreNotSet() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(JSON_VERSION_KEY, 1);
        RulesetSchemaData rulesetSchemaData = new RulesetSchemaData(schemaData);

        // test
        Object content = rulesetSchemaData.getContent();

        // verify
        assertNull(content);
    }
}
