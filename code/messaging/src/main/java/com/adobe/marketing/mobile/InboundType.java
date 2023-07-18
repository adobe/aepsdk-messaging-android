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

package com.adobe.marketing.mobile;

public enum InboundType {
    // Unknown inbound type
    UNKNOWN(0),
    // Feed Item
    FEED(1),
    // InApp
    INAPP(2);

    static final String UNKNOWN_EVENT_TYPE = "unknown";
    static final String FEED_EVENT_TYPE = "feed";
    static final String INAPP_EVENT_TYPE = "inapp";

    final int value;

    InboundType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getInboundEventType() {
        switch (this) {
            case FEED:
                return FEED_EVENT_TYPE;
            case INAPP:
                return INAPP_EVENT_TYPE;
            default:
                return UNKNOWN_EVENT_TYPE;
        }
    }
}
