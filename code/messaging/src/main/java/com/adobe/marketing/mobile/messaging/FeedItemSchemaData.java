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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

// represents the schema data object for a feed item schema
public class FeedItemSchemaData implements SchemaData {
    private static final String SELF_TAG = "FeedItemSchemaData";
    private Object content;
    private ContentType contentType;
    private int publishedDate;
    private int expiryDate;
    private Map<String, Object> meta;

    FeedItemSchemaData(final JSONObject schemaData) {
        try {
            this.contentType =
                    ContentType.fromString(
                            schemaData.optString(
                                    MessagingConstants.ConsequenceDetailDataKeys.CONTENT_TYPE));
            if (contentType.equals(ContentType.APPLICATION_JSON)) {
                this.content =
                        JSONUtils.toMap(
                                schemaData.getJSONObject(
                                        MessagingConstants.ConsequenceDetailDataKeys.CONTENT));
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
        } catch (final JSONException exception) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating FeedItemSchemaData from json object: %s",
                    exception.getLocalizedMessage());
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

    @VisibleForTesting
    static FeedItemSchemaData getEmpty() {
        return new FeedItemSchemaData(new JSONObject());
    }

    @Nullable public FeedItem getFeedItem() {
        if (!contentType.equals(ContentType.APPLICATION_JSON)) {
            return null;
        }

        try {
            final Map<String, Object> contentMap = (HashMap<String, Object>) content;
            final String title =
                    DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.TITLE, "");
            final String body =
                    DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.BODY, "");
            final String imageUrl =
                    DataReader.optString(
                            contentMap, MessagingConstants.MessageFeedKeys.IMAGE_URL, "");
            final String actionUrl =
                    DataReader.optString(
                            contentMap, MessagingConstants.MessageFeedKeys.ACTION_URL, "");
            final String actionTitle =
                    DataReader.optString(
                            contentMap, MessagingConstants.MessageFeedKeys.ACTION_TITLE, "");
            return new FeedItem.Builder(title, body)
                    .setImageUrl(imageUrl)
                    .setActionUrl(actionUrl)
                    .setActionTitle(actionTitle)
                    .setParent(this)
                    .build();
        } catch (final ClassCastException exception) {
            return null;
        }
    }
}
