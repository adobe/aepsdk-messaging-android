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
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonContentSchemaData implements SchemaData {
    private static final String SELF_TAG = "JsonContentSchemaData";
    private Object content = null;
    private ContentType format = null;

    JsonContentSchemaData(final JSONObject schemaData) {
        try {
            final String decodedFormat =
                    schemaData.optString(MessagingConstants.ConsequenceDetailDataKeys.FORMAT);
            if (StringUtils.isNullOrEmpty(decodedFormat)) {
                format = ContentType.APPLICATION_JSON;
            } else {
                format = ContentType.fromString(decodedFormat);
            }
            try {
                this.content =
                        JSONUtils.toMap(
                                schemaData.getJSONObject(
                                        MessagingConstants.ConsequenceDetailDataKeys.CONTENT));
            } catch (JSONException e) {
                this.content =
                        JSONUtils.toList(
                                schemaData.getJSONArray(
                                        MessagingConstants.ConsequenceDetailDataKeys.CONTENT));
            }
        } catch (final JSONException jsonException) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating HtmlContentSchemaData from json object: %s",
                    jsonException.getLocalizedMessage());
        }
    }

    @Nullable Map<String, Object> getJsonObjectContent() {
        return content instanceof Map ? (Map<String, Object>) content : null;
    }

    @Nullable List<Map<String, Object>> getJsonArrayContent() {
        return content instanceof List ? (List<Map<String, Object>>) content : null;
    }

    ContentType getFormat() {
        return format;
    }

    @Override
    @Nullable public Object getContent() {
        return content;
    }
}
