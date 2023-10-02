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

enum InboundType {
    // Unknown inbound type
    UNKNOWN(0),
    // Feed Item
    FEED(1),
    // InApp
    INAPP(2);

    static final String SCHEMA_FEED_ITEM = "https://ns.adobe.com/personalization/message/feed-item";
    static final String SCHEMA_IAM = "https://ns.adobe.com/personalization/message/in-app";
    static final String SCHEMA_UNKNOWN = "unknown";

    private final int value;

    InboundType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        switch (this) {
            case FEED:
                return SCHEMA_FEED_ITEM;
            case INAPP:
                return SCHEMA_IAM;
            default:
                return SCHEMA_UNKNOWN;
        }
    }

    static InboundType fromString(final String typeString) {
        InboundType inboundType;
        switch (typeString) {
            case SCHEMA_FEED_ITEM:
                inboundType = InboundType.FEED;
                break;
            case SCHEMA_IAM:
                inboundType = InboundType.INAPP;
                break;
            default:
                inboundType = InboundType.UNKNOWN;
        }
        return inboundType;
    }
}