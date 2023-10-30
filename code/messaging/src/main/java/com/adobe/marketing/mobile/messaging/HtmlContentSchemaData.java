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

import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

// represents the schema data object for a html content schema
public class HtmlContentSchemaData implements Serializable {
    private static final String FORMAT = "format";
    private final String content;
    private final ContentType contentType;

    HtmlContentSchemaData(final JSONObject jsonObject) {
        final String decodedFormat = jsonObject.optString(FORMAT);
        if (StringUtils.isNullOrEmpty(decodedFormat)) {
            contentType = ContentType.TEXT_HTML;
        } else {
            contentType = ContentType.fromString(decodedFormat);
        }
        this.content = jsonObject.optString(CONTENT);
    }

    public Object getContent() {
        if (contentType.equals(ContentType.APPLICATION_JSON)) {
            try {
                return new JSONObject(content);
            } catch (final JSONException jsonException) {
                return null;
            }
        } else {
            return content;
        }
    }

    public ContentType getContentType() {
        return contentType;
    }
}
