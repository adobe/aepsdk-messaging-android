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

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.CONTENT;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.DATA;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.SCHEMA;

import java.io.Serializable;
import java.util.Map;

class PayloadItem implements Serializable {
    final private String id;
    final private String schema;
    final private ItemData data;

    PayloadItem(final Map<String, Object> payloadItemMap) {
        this.id = (String) payloadItemMap.get(ID);
        this.schema = (String) payloadItemMap.get(SCHEMA);
        final Map<String, String> dataMap = (Map<String, String>) payloadItemMap.get(DATA);
        this.data = new ItemData(dataMap.get(ID), dataMap.get(CONTENT));
    }

    String getId() {
        return id;
    }

    String getSchema() {
        return schema;
    }

    ItemData getData() {
        return data;
    }
}
