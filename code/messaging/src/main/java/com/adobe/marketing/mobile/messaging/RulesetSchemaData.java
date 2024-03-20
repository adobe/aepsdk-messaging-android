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

import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.services.Log;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class RulesetSchemaData implements SchemaData {
    private static final String SELF_TAG = "RulesetSchemaData";
    private int version = 0;
    private List<Map<String, Object>> rules = null;

    RulesetSchemaData(final JSONObject schemaData) {
        try {
            this.version =
                    schemaData.getInt(
                            MessagingConstants.EventDataKeys.RulesEngine.JSON_VERSION_KEY);
            this.rules =
                    (List<Map<String, Object>>)
                            schemaData.get(
                                    MessagingConstants.EventDataKeys.RulesEngine.JSON_RULES_KEY);
        } catch (final JSONException | ClassCastException exception) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating RulesetSchemaData from json object: %s",
                    exception.getLocalizedMessage());
        }
    }

    public int getVersion() {
        return version;
    }

    @Nullable public List<Map<String, Object>> getRules() {
        return rules;
    }

    @Override
    @Nullable public Object getContent() {
        return rules;
    }
}
