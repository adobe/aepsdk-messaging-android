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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SchemaTypeTests {

    @Test
    public void getValue_setsValueCorrectly_forHtmlContent() {
        // setup
        int expectedValue = 1;

        // test
        SchemaType schemaType = SchemaType.HTML_CONTENT;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forJsonContent() {
        // setup
        int expectedValue = 2;

        // test
        SchemaType schemaType = SchemaType.JSON_CONTENT;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forRuleset() {
        // setup
        int expectedValue = 3;

        // test
        SchemaType schemaType = SchemaType.RULESET;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forInapp() {
        // setup
        int expectedValue = 4;

        // test
        SchemaType schemaType = SchemaType.INAPP;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forFeed() {
        // setup
        int expectedValue = 5;

        // test
        SchemaType schemaType = SchemaType.FEED;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forNativeAlert() {
        // setup
        int expectedValue = 6;

        // test
        SchemaType schemaType = SchemaType.NATIVE_ALERT;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forDefault() {
        // setup
        int expectedValue = 7;

        // test
        SchemaType schemaType = SchemaType.DEFAULT_CONTENT;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void getValue_setsValueCorrectly_forUnknown() {
        // setup
        int expectedValue = 0;

        // test
        SchemaType schemaType = SchemaType.UNKNOWN;

        // verify
        assertEquals(expectedValue, schemaType.getValue());
    }

    @Test
    public void toString_returnsCorrectString_forHtmlContent() {
        // setup
        SchemaType schemaType = SchemaType.HTML_CONTENT;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_HTML_CONTENT, result);
    }

    @Test
    public void toString_returnsCorrectString_forJsonContent() {
        // setup
        SchemaType schemaType = SchemaType.JSON_CONTENT;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_JSON_CONTENT, result);
    }

    @Test
    public void toString_returnsCorrectString_forRuleset() {
        // setup
        SchemaType schemaType = SchemaType.RULESET;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_RULESET_ITEM, result);
    }

    @Test
    public void toString_returnsCorrectString_forInapp() {
        // setup
        SchemaType schemaType = SchemaType.INAPP;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_IAM, result);
    }

    @Test
    public void toString_returnsCorrectString_forFeed() {
        // setup
        SchemaType schemaType = SchemaType.FEED;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_FEED_ITEM, result);
    }

    @Test
    public void toString_returnsCorrectString_forNativeAlert() {
        // setup
        SchemaType schemaType = SchemaType.NATIVE_ALERT;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_NATIVE_ALERT, result);
    }

    @Test
    public void toString_returnsCorrectString_forDefault() {
        // setup
        SchemaType schemaType = SchemaType.DEFAULT_CONTENT;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals(MessagingTestConstants.SchemaValues.SCHEMA_DEFAULT_CONTENT, result);
    }

    @Test
    public void toString_returnsEmptyString_forUnknown() {
        // setup
        SchemaType schemaType = SchemaType.UNKNOWN;

        // test
        String result = schemaType.toString();

        // verify
        assertEquals("", result);
    }

    @Test
    public void fromString_returnsCorrectEnum_forHtmlContent() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_HTML_CONTENT;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.HTML_CONTENT, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forJsonContent() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_JSON_CONTENT;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.JSON_CONTENT, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forRuleset() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_RULESET_ITEM;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.RULESET, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forInapp() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_IAM;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.INAPP, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forFeed() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_FEED_ITEM;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.FEED, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forNativeAlert() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_NATIVE_ALERT;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.NATIVE_ALERT, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forDefault() {
        // setup
        String typeString = MessagingTestConstants.SchemaValues.SCHEMA_DEFAULT_CONTENT;

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.DEFAULT_CONTENT, schemaType);
    }

    @Test
    public void fromString_returnsCorrectEnum_forUnknown() {
        // setup
        String typeString = "randomString";

        // test
        SchemaType schemaType = SchemaType.fromString(typeString);

        // verify
        assertEquals(SchemaType.UNKNOWN, schemaType);
    }

    @Test
    public void fromString_returnsUnknown_forNull() {
        // test
        SchemaType schemaType = SchemaType.fromString(null);

        // verify
        assertEquals(SchemaType.UNKNOWN, schemaType);
    }
}
