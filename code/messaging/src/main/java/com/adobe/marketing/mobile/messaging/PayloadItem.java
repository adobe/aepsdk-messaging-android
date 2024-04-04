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

package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.Serializable;
import java.util.Map;

class PayloadItem implements Serializable {
    final String id;
    final String schema;
    final Map<String, Object> data;

    PayloadItem(final Map<String, Object> payloadItemMap) throws Exception {
        id = DataReader.getString(payloadItemMap, MessagingConstants.PayloadKeys.ID);
        schema = DataReader.getString(payloadItemMap, MessagingConstants.PayloadKeys.SCHEMA);
        data =
                DataReader.getTypedMap(
                        Object.class, payloadItemMap, MessagingConstants.PayloadKeys.DATA);
        if (StringUtils.isNullOrEmpty(id)
                || StringUtils.isNullOrEmpty(schema)
                || MapUtils.isNullOrEmpty(data)) {
            throw new Exception(
                    "id, schema, and data are required for constructing PayloadItem objects.");
        }
    }
}
