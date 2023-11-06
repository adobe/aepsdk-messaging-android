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
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.MessageFeedKeys.ACTION_TITLE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.MessageFeedKeys.ACTION_URL;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.MessageFeedKeys.BODY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.MessageFeedKeys.IMAGE_URL;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.MessageFeedKeys.TITLE;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

// represents the schema data object for a feed item schema
public class FeedItemSchemaData {
    private static final String SELF_TAG = "FeedItemSchemaData";
    private Object content;
    private ContentType contentType;
    private int publishedDate;
    private int expiryDate;
    private Map<String, Object> meta;

    FeedItemSchemaData(final JSONObject schemaData) {
        try {
            final JSONObject consequenceDetails = InternalMessagingUtils.getConsequenceDetails(schemaData);
            final Map<String, Object> feedItemData = JSONUtils.toMap(consequenceDetails.getJSONObject(DATA));
            this.contentType = ContentType.fromString(DataReader.getString(feedItemData, CONTENT_TYPE));
            if (contentType.equals(ContentType.APPLICATION_JSON)) {
                this.content = DataReader.getTypedMap(Object.class, feedItemData, CONTENT);
            } else {
                this.content = DataReader.getString(feedItemData, CONTENT);
            }
            this.publishedDate = DataReader.optInt(feedItemData, PUBLISHED_DATE, 0);
            this.expiryDate = DataReader.optInt(feedItemData, EXPIRY_DATE, 0);
            this.meta = DataReader.optTypedMap(Object.class, feedItemData, METADATA, Collections.emptyMap());
        } catch (final JSONException | DataReaderException exception) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating FeedItemSchemaData from json object: %s", exception.getLocalizedMessage());
        }
    }

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

    public Map<String, Object> getMeta() {
        return meta;
    }

    public FeedItemSchemaData getEmpty() {
        return new FeedItemSchemaData(new JSONObject());
    }

    public FeedItem getFeedItem() {
        if (!contentType.equals(ContentType.APPLICATION_JSON)) {
            return null;
        }

        final JSONObject jsonContent = (JSONObject) content;
        final String title = jsonContent.optString(TITLE);
        final String body = jsonContent.optString(BODY);
        final String imageUrl = jsonContent.optString(IMAGE_URL);
        final String actionUrl = jsonContent.optString(ACTION_URL);
        final String actionTitle = jsonContent.optString(ACTION_TITLE);
        return new FeedItem.Builder(title, body)
                .setImageUrl(imageUrl)
                .setActionUrl(actionUrl)
                .setActionTitle(actionTitle)
                .setParent(this)
                .build();
    }
}
