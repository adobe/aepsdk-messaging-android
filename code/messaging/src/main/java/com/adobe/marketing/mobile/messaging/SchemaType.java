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

import androidx.annotation.NonNull;

public enum SchemaType {
    UNKNOWN(0),
    HTML_CONTENT(1),
    JSON_CONTENT(2),
    RULESET(3),
    INAPP(4),
    FEED(5),
    NATIVE_ALERT(6),
    DEFAULT_CONTENT(7);

    private final int value;

    SchemaType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull @Override
    public String toString() {
        switch (this) {
            case HTML_CONTENT:
                return MessagingConstants.SchemaValues.SCHEMA_HTML_CONTENT;
            case JSON_CONTENT:
                return MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT;
            case RULESET:
                return MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM;
            case INAPP:
                return MessagingConstants.SchemaValues.SCHEMA_IAM;
            case FEED:
                return MessagingConstants.SchemaValues.SCHEMA_FEED_ITEM;
            case NATIVE_ALERT:
                return MessagingConstants.SchemaValues.SCHEMA_NATIVE_ALERT;
            case DEFAULT_CONTENT:
                return MessagingConstants.SchemaValues.SCHEMA_DEFAULT_CONTENT;
            default:
                return "";
        }
    }

    static SchemaType fromString(final String typeString) {
        if (typeString == null) {
            return UNKNOWN;
        }
        SchemaType schemaType;
        switch (typeString) {
            case MessagingConstants.SchemaValues.SCHEMA_HTML_CONTENT:
                schemaType = SchemaType.HTML_CONTENT;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT:
                schemaType = SchemaType.JSON_CONTENT;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM:
                schemaType = SchemaType.RULESET;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_IAM:
                schemaType = SchemaType.INAPP;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_FEED_ITEM:
                schemaType = SchemaType.FEED;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_NATIVE_ALERT:
                schemaType = SchemaType.NATIVE_ALERT;
                break;
            case MessagingConstants.SchemaValues.SCHEMA_DEFAULT_CONTENT:
                schemaType = SchemaType.DEFAULT_CONTENT;
                break;
            default:
                schemaType = SchemaType.UNKNOWN;
        }
        return schemaType;
    }
}
