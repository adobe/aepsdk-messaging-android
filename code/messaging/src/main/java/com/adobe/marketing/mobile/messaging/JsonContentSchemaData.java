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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class JsonContentSchemaData {
    private static final String SELF_TAG = "JsonContentSchemaData";
    private static final String FORMAT = "format";
    private Object content = null;
    private ContentType format = null;

    JsonContentSchemaData(final JSONObject schemaData) {
        try {
            final String decodedFormat = schemaData.optString(FORMAT);
            if (StringUtils.isNullOrEmpty(decodedFormat)) {
                format = ContentType.APPLICATION_JSON;
            } else {
                format = ContentType.fromString(decodedFormat);
            }
            final Object content = schemaData.get(CONTENT);
            if (content instanceof JSONObject) {
                this.content = JSONUtils.toMap((JSONObject) content);
            } else if (content instanceof JSONArray) {
                this.content = JSONUtils.toList((JSONArray) content);
            } else { // we have non json content, just store it as a string
                this.content = content.toString();
            }
        } catch (final JSONException jsonException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating HtmlContentSchemaData from json object: %s", jsonException.getLocalizedMessage());
        }
    }

    Map<String, Object> getJsonObjectContent() {
        return content instanceof Map ? (Map<String, Object>) content : null;
    }

    List<Map<String, Object>> getJsonArrayContent() {
        return content instanceof List ? (List<Map<String, Object>>) content : null;
    }

    ContentType getFormat() {
        return format;
    }
}
