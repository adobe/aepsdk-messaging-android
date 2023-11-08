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
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.METADATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.WEB_PARAMETERS;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// represents the schema data object for an in-app schema
public class InAppSchemaData implements SchemaData {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "InAppSchemaData";
    private Object content;
    private ContentType contentType;
    private int publishedDate;
    private int expiryDate;
    private Map<String, Object> meta;
    private Map<String, Object> mobileParameters;
    private Map<String, Object> webParameters;
    private List<String> remoteAssets = new ArrayList<>();

    InAppSchemaData(final JSONObject schemaData) {
        try {
            this.contentType = ContentType.fromString(schemaData.optString(CONTENT_TYPE));
            if (contentType.equals(ContentType.APPLICATION_JSON)) {
                this.content = JSONUtils.toMap(schemaData.getJSONObject(CONTENT));
            } else {
                this.content = schemaData.getString(CONTENT);
            }
            this.publishedDate = schemaData.optInt(PUBLISHED_DATE);
            this.expiryDate = schemaData.optInt(EXPIRY_DATE);
            this.meta = JSONUtils.toMap(schemaData.optJSONObject(METADATA));
            this.mobileParameters = JSONUtils.toMap(schemaData.optJSONObject(MOBILE_PARAMETERS));
            this.webParameters = JSONUtils.toMap(schemaData.optJSONObject(WEB_PARAMETERS));
            final List<Object> assetList = JSONUtils.toList(schemaData.optJSONArray(REMOTE_ASSETS));
            if (!MessagingUtils.isNullOrEmpty(assetList)) {
                for (final Object asset : assetList) {
                    this.remoteAssets.add(asset.toString());
                }
            }
        } catch (final JSONException jsonException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating InAppSchemaData from json object: %s", jsonException.getLocalizedMessage());
        }
    }

    @Override
    public Object getContent() {
        if (contentType.equals(ContentType.APPLICATION_JSON)) {
            try {
                return new JSONObject(content.toString());
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

    public int getPublishedDate() {
        return publishedDate;
    }

    public int getExpiryDate() {
        return expiryDate;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public Map<String, Object> getMobileParameters() {
        return mobileParameters;
    }

    public Map<String, Object> getWebParameters() {
        return webParameters;
    }

    public List<String> getRemoteAssets() {
        return remoteAssets;
    }
}
