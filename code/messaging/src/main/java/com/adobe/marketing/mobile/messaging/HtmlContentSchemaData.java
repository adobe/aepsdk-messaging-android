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
import com.adobe.marketing.mobile.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

// represents the schema data object for a html content schema
public class HtmlContentSchemaData implements SchemaData {
    private static final String SELF_TAG = "HtmlContentSchemaData";
    private String content = null;
    private ContentType format = null;

    HtmlContentSchemaData(final JSONObject schemaData) {
        try {
            final String decodedFormat =
                    schemaData.optString(MessagingConstants.ConsequenceDetailDataKeys.FORMAT);
            if (StringUtils.isNullOrEmpty(decodedFormat)) {
                format = ContentType.TEXT_HTML;
            } else {
                format = ContentType.fromString(decodedFormat);
            }
            this.content =
                    schemaData.getString(MessagingConstants.ConsequenceDetailDataKeys.CONTENT);
        } catch (final JSONException jsonException) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating HtmlContentSchemaData from json object: %s",
                    jsonException.getLocalizedMessage());
        }
    }

    @Override
    @Nullable public String getContent() {
        return content;
    }

    public ContentType getFormat() {
        return format;
    }
}
