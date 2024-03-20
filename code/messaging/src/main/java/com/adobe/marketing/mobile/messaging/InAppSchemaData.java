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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

// represents the schema data object for an in-app schema
public class InAppSchemaData implements SchemaData {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "InAppSchemaData";
    private Object content = null;
    private ContentType contentType = null;
    private int publishedDate = 0;
    private int expiryDate = 0;
    private Map<String, Object> meta = null;
    private Map<String, Object> mobileParameters = null;
    private Map<String, Object> webParameters = null;
    private List<String> remoteAssets = null;

    InAppSchemaData(final JSONObject schemaData) {
        try {
            String contentTypeString =
                    schemaData.optString(MessagingConstants.ConsequenceDetailDataKeys.CONTENT_TYPE);
            if (StringUtils.isNullOrEmpty(contentTypeString)) {
                this.contentType = ContentType.fromString(contentTypeString);
                return;
            }
            this.contentType = ContentType.fromString(contentTypeString);
            if (contentType.equals(ContentType.APPLICATION_JSON)) {
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
            } else {
                this.content =
                        schemaData.getString(MessagingConstants.ConsequenceDetailDataKeys.CONTENT);
            }
            this.publishedDate =
                    schemaData.optInt(MessagingConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE);
            this.expiryDate =
                    schemaData.optInt(MessagingConstants.ConsequenceDetailDataKeys.EXPIRY_DATE);
            this.meta =
                    JSONUtils.toMap(
                            schemaData.optJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys.METADATA));
            this.mobileParameters =
                    JSONUtils.toMap(
                            schemaData.optJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys
                                            .MOBILE_PARAMETERS));
            this.webParameters =
                    JSONUtils.toMap(
                            schemaData.optJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys.WEB_PARAMETERS));
            final List<Object> assetList =
                    JSONUtils.toList(
                            schemaData.optJSONArray(
                                    MessagingConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS));
            if (!MessagingUtils.isNullOrEmpty(assetList)) {
                this.remoteAssets = new ArrayList<>();
                for (final Object asset : assetList) {
                    this.remoteAssets.add(asset.toString());
                }
            }
        } catch (final JSONException jsonException) {
            Log.trace(
                    LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating InAppSchemaData from json object: %s",
                    jsonException.getLocalizedMessage());
        }
    }

    @Override
    public Object getContent() {
        return content;
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

    @Nullable public Map<String, Object> getMeta() {
        return meta;
    }

    @Nullable public Map<String, Object> getMobileParameters() {
        return mobileParameters;
    }

    @Nullable public Map<String, Object> getWebParameters() {
        return webParameters;
    }

    @Nullable public List<String> getRemoteAssets() {
        return remoteAssets;
    }
}
