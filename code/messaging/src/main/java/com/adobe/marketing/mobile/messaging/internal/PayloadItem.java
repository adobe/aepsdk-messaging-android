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

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.DATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.SCHEMA;

import com.adobe.marketing.mobile.util.DataReader;

import java.io.Serializable;
import java.util.Map;

class PayloadItem implements Serializable {
    final String id;
    final String schema;
    final ItemData data;

    PayloadItem(final Map<String, Object> payloadItemMap) {
        id = DataReader.optString(payloadItemMap, ID, "");
        schema = DataReader.optString(payloadItemMap, SCHEMA, "");
        final Map<String, String> dataMap = DataReader.optTypedMap(String.class, payloadItemMap, DATA, null);
        data = new ItemData(DataReader.optString(dataMap, ID, ""), DataReader.optString(dataMap, CONTENT, ""));
    }
}