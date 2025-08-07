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

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject

object ContentCardJsonDataUtils {
    internal val BASE_JSON = JSONObject(
        """
        {
          "id": "1c1fb7c4-f3e7-4766-a782-ec5b3c87a62e",
          "type": "schema",
          "detail": {
            "id": "1c1fb7c4-f3e7-4766-a782-ec5b3c87a62e",
            "schema": "https://ns.adobe.com/personalization/message/content-card",
            "data": {
              "content": {
                "actionUrl": "",
                "body": {
                  "content": "Content card testing triggers track action \"smoke_test\""
                },
                "buttons": [
                  {
                    "interactId": "buttonID1",
                    "actionUrl": "https://adobe.com/offer",
                    "text": {
                      "content": "Purchase Now"
                    }
                  }
                ],
                "image": {
                  "alt": "",
                  "url": "https://i.ibb.co/0X8R3TG/Messages-24.png"
                },
                "dismissBtn": {
                  "style": "none"
                },
                "title": {
                  "content": "Messaging SDK Smoke Test"
                }
              },
              "contentType": "application/json",
              "meta": {
                "adobe": {
                  "template": "SmallImage"
                },
                "surface": "mobileapp://com.adobe.marketing.mobile.messagingsample/card/ms"
              }
            }
          }
        }
    """
    )

    internal val baseJsonMap = JSONUtils.toMap(BASE_JSON) ?: emptyMap()
    internal val contentMap = (baseJsonMap["detail"] as Map<*, *>)["data"] as Map<String, Any>
    internal val metaMap = contentMap["meta"] as Map<String, Any>
    internal val contentCardMap = contentMap["content"] as Map<String, Any>
}
