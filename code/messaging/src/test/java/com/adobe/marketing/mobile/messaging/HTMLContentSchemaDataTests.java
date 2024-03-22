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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.services.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class HTMLContentSchemaDataTests {

    @Test
    public void constructor_setsFieldsCorrectly_whenFormatIsMissing() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, "content");

        // test
        HtmlContentSchemaData htmlContentSchemaData = new HtmlContentSchemaData(schemaData);

        // verify
        assertEquals(ContentType.TEXT_HTML, htmlContentSchemaData.getFormat());
        assertEquals("content", htmlContentSchemaData.getContent());
    }

    @Test
    public void constructor_setsFieldsCorrectly_whenFormatIsApplicationJson() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, "{\"key\":\"value\"}");

        // test
        HtmlContentSchemaData htmlContentSchemaData = new HtmlContentSchemaData(schemaData);

        // verify
        assertEquals(ContentType.APPLICATION_JSON, htmlContentSchemaData.getFormat());
        assertEquals("{\"key\":\"value\"}", htmlContentSchemaData.getContent());
    }

    @Test
    public void constructor_setsFieldsCorrectly_whenFormatIsTextHtml() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.TEXT_HTML);
        schemaData.put(CONTENT, "<h1>content</h1>");

        // test
        HtmlContentSchemaData htmlContentSchemaData = new HtmlContentSchemaData(schemaData);

        // verify
        assertEquals(ContentType.TEXT_HTML, htmlContentSchemaData.getFormat());
        assertEquals("<h1>content</h1>", htmlContentSchemaData.getContent());
    }

    @Test
    public void constructor_logsException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

            // test
            HtmlContentSchemaData htmlContentSchemaData = new HtmlContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, htmlContentSchemaData.getFormat());
            assertNull(htmlContentSchemaData.getContent());
        }
    }

    @Test
    public void constructor_logsException_whenContentIsNotString() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
            schemaData.put(CONTENT, new JSONObject().put("key", "value"));

            // test
            HtmlContentSchemaData htmlContentSchemaData = new HtmlContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, htmlContentSchemaData.getFormat());
            assertNull(htmlContentSchemaData.getContent());
        }
    }
}
