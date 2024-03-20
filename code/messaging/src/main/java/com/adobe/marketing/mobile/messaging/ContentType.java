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

public enum ContentType {
    APPLICATION_JSON(0),
    TEXT_HTML(1),
    TEXT_XML(2),
    TEXT_PLAIN(3),
    UNKNOWN(4);

    private final int value;

    ContentType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        switch (this) {
            case APPLICATION_JSON:
                return MessagingConstants.ContentTypes.APPLICATION_JSON;
            case TEXT_HTML:
                return MessagingConstants.ContentTypes.TEXT_HTML;
            case TEXT_XML:
                return MessagingConstants.ContentTypes.TEXT_XML;
            case TEXT_PLAIN:
                return MessagingConstants.ContentTypes.TEXT_PLAIN;
            default:
                return "";
        }
    }

    static ContentType fromString(final String typeString) {
        if (typeString == null) {
            return UNKNOWN;
        }
        ContentType contentType;
        switch (typeString) {
            case MessagingConstants.ContentTypes.APPLICATION_JSON:
                contentType = ContentType.APPLICATION_JSON;
                break;
            case MessagingConstants.ContentTypes.TEXT_HTML:
                contentType = ContentType.TEXT_HTML;
                break;
            case MessagingConstants.ContentTypes.TEXT_XML:
                contentType = ContentType.TEXT_XML;
                break;
            case MessagingConstants.ContentTypes.TEXT_PLAIN:
                contentType = ContentType.TEXT_PLAIN;
                break;
            default:
                contentType = ContentType.UNKNOWN;
        }
        return contentType;
    }
}
