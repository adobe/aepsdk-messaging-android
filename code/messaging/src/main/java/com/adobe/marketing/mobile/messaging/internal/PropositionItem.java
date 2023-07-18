/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.Inbound;
import com.adobe.marketing.mobile.PropositionEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import org.json.JSONException;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;

class PropositionItem implements Serializable {
    private final static String SELF_TAG = "Proposition";
    // Unique proposition identifier
    String uniqueId;
    // PropositionItem schema string
    String schema;
    // PropositionItem data content e.g. html or plain-text string or string containing image URL, JSON string
    String content;
    // Weak reference to Proposition instance
    WeakReference<Proposition> proposition;

    PropositionItem(final Map<String, Object> item) throws DataReaderException {
        this.uniqueId = DataReader.getString(item, "id");
        this.schema = DataReader.getString(item, "schema");
        final Map data = DataReader.getTypedMap(Object.class, item, "data");
        this.content = DataReader.getString(data, "content");
    }

    // Track offer interaction
    void track(final PropositionEventType interaction) {

    }

    // Decode data content to generic inbound
    Inbound decodeContent() {
        try {
            return new Inbound(content);
        } catch (final JSONException e) {
            Log.trace(LOG_TAG, SELF_TAG, "JSONException thrown while attempting to decode content: %s", e.getLocalizedMessage());
            return null;
        }
    }
}