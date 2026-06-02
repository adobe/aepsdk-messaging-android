/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.Map;
import org.json.JSONObject;

public class InboxContentSchemaData implements SchemaData {
    private static final String SELF_TAG = "InboxContentSchemaData";
    private Map<String, Object> content = null;
    private Map<String, Object> metadata = null;

    InboxContentSchemaData(final JSONObject schemaData) {
        try {
            this.content =
                    JSONUtils.toMap(
                            schemaData.getJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys.CONTENT));
            this.metadata =
                    JSONUtils.toMap(
                            schemaData.getJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys.METADATA));
        } catch (final Exception exception) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating InboxContentSchemaData from json object: %s",
                    exception.getLocalizedMessage());
        }
    }

    @Override
    public Map<String, Object> getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
