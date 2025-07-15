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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import org.json.JSONObject;

// represents the schema data object for a event history operation schema
class EventHistoryOperationSchemaData implements SchemaData {
    private static final String SELF_TAG = "ContentCardSchemaData";
    PropositionItem parent;
    private Object content;
    private String operation;

    EventHistoryOperationSchemaData(final JSONObject schemaData) {
        try {
            this.operation =
                    schemaData.getString(MessagingConstants.ConsequenceDetailDataKeys.OPERATION);
            this.content =
                    JSONUtils.toMap(
                            schemaData.getJSONObject(
                                    MessagingConstants.ConsequenceDetailDataKeys.CONTENT));
        } catch (final Exception exception) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Error parsing EventHistoryOperationSchemaData: %s",
                    exception.getMessage());
        }
    }

    @Override
    public Object getContent() {
        return content;
    }

    public String getOperation() {
        return operation;
    }
}
