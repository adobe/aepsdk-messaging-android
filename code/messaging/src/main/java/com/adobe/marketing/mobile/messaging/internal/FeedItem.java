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

package com.adobe.marketing.mobile.messaging.internal;

import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

class FeedItem implements Serializable {
    // Plain-text title for the feed item
    String title;
    // Plain-text body representing the content for the feed item
    String body;
    // String representing a URI that contains an image to be used for this feed item
    String imageUrl;
    // Contains a URL to be opened if the user interacts with the feed item
    String actionUrl;
    // Required if actionUrl is provided. Text to be used in title of button or link in feed item
    String actionTitle;
    // Represents when this feed item went live. Represented in seconds since January 1, 1970
    long publishedDate;
    // Represents when this feed item expires. Represented in seconds since January 1, 1970
    long expiryDate;
    // Contains additional key-value pairs associated with this feed item
    Map<String, Object> meta;

    private FeedItem(final String title, final String body, final String imageUrl, final String actionUrl, final String actionTitle, final long publishedDate, final long expiryDate, final Map<String, Object> meta) {
        this.title = title;
        this.body = body;
        this.imageUrl = imageUrl;
        this.actionUrl = actionUrl;
        this.actionTitle = actionTitle;
        this.publishedDate = publishedDate;
        this.expiryDate = expiryDate;
        this.meta = meta;
    }

    static FeedItem create(final String title, final String body, final String imageUrl, final String actionUrl, final String actionTitle, final long publishedDate, final long expiryDate, final Map<String, Object> meta) {
        if (StringUtils.isNullOrEmpty(title) || StringUtils.isNullOrEmpty(body) || publishedDate <= 0 || expiryDate <= 0) {
            return null;
        }

        return new FeedItem(title, body, imageUrl, actionUrl, actionTitle, publishedDate, expiryDate, meta);
    }

    private void writeObject(final ObjectOutputStream objectOutputStream)
            throws IOException {
        objectOutputStream.writeUTF(title);
        objectOutputStream.writeUTF(body);
        objectOutputStream.writeUTF(imageUrl);
        objectOutputStream.writeUTF(actionTitle);
        objectOutputStream.writeUTF(actionUrl);
        objectOutputStream.writeLong(publishedDate);
        objectOutputStream.writeLong(expiryDate);
        final String mapAsJsonString = new JSONObject(meta).toString();
        objectOutputStream.writeUTF(mapAsJsonString);
    }

    private void readObject(final ObjectInputStream objectInputStream)
            throws ClassNotFoundException, IOException, JSONException {
        this.title = objectInputStream.readUTF();
        this.body = objectInputStream.readUTF();
        this.imageUrl = objectInputStream.readUTF();
        this.actionTitle = objectInputStream.readUTF();
        this.actionUrl = objectInputStream.readUTF();
        this.publishedDate = objectInputStream.readLong();
        this.expiryDate = objectInputStream.readLong();
        final JSONObject mapAsJson = new JSONObject(objectInputStream.readUTF());
        this.meta = JSONUtils.toMap(mapAsJson);
    }
}
