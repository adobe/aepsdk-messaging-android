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
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// represents the schema data object for a html content schema
public class HtmlContentSchemaData {
    private static final String SELF_TAG = "HtmlContentSchemaData";
    private static final String FORMAT = "format";
    private String content = null;
    private ContentType format = null;

    HtmlContentSchemaData(final JSONObject schemaData) {
        try {
            final String decodedFormat = schemaData.optString(FORMAT);
            if (StringUtils.isNullOrEmpty(decodedFormat)) {
                format = ContentType.TEXT_HTML;
            } else {
                format = ContentType.fromString(decodedFormat);
            }
            this.content = schemaData.getString(CONTENT);
        } catch (final JSONException jsonException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating HtmlContentSchemaData from json object: %s", jsonException.getLocalizedMessage());
        }
    }

    public String getContent() {
        if (format.equals(ContentType.APPLICATION_JSON)) {
            try {
                if (content.startsWith("[")) {
                    return new JSONArray(content).toString();
                } else {
                    return new JSONObject(content).toString();
                }
            } catch (final JSONException jsonException) {
                return null;
            }
        } else {
            return content;
        }
    }

    public ContentType getFormat() {
        return format;
    }
}
