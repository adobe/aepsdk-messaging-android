/*
  Copyright 2026 Adobe. All rights reserved.
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
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.METADATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.services.Log;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class InboxContentSchemaDataTests {

    @Test
    public void constructor_setsAllFieldsCorrectly_whenContentAndMetadataAreValid()
            throws JSONException {
        // setup
        JSONObject schemaData = createValidInboxSchemaObject();

        // test
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // verify
        assertNotNull(inboxContentSchemaData.getContent());
        assertTrue(inboxContentSchemaData.getContent() instanceof Map);
        assertEquals("My Inbox", ((Map) inboxContentSchemaData.getContent()).get("heading"));
        assertEquals(10, ((Map) inboxContentSchemaData.getContent()).get("capacity"));

        assertNotNull(inboxContentSchemaData.getMetadata());
        assertTrue(inboxContentSchemaData.getMetadata() instanceof Map);
        assertEquals("metaValue", inboxContentSchemaData.getMetadata().get("metaKey"));
    }

    @Test
    public void constructor_setsContentCorrectly_whenMetadataIsMissing() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        JSONObject contentObject = new JSONObject();
        contentObject.put("heading", "My Inbox");
        contentObject.put("capacity", 10);
        schemaData.put(CONTENT, contentObject);

        // test
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // verify
        assertNotNull(inboxContentSchemaData.getContent());
        assertTrue(inboxContentSchemaData.getContent() instanceof Map);
        assertEquals("My Inbox", ((Map) inboxContentSchemaData.getContent()).get("heading"));
        assertEquals(10, ((Map) inboxContentSchemaData.getContent()).get("capacity"));
        assertNull(inboxContentSchemaData.getMetadata());
    }

    @Test
    public void constructor_setsMetadataCorrectly_whenContentIsMissing() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        JSONObject metadataObject = new JSONObject();
        metadataObject.put("metaKey", "metaValue");
        schemaData.put(METADATA, metadataObject);

        // test
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // verify
        assertNull(inboxContentSchemaData.getContent());
        assertNull(inboxContentSchemaData.getMetadata());
    }

    @Test
    public void constructor_handlesException_whenContentIsNotJSONObject() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT, "invalidContent");
            JSONObject metadataObject = new JSONObject();
            metadataObject.put("metaKey", "metaValue");
            schemaData.put(METADATA, metadataObject);

            // test
            InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertNull(inboxContentSchemaData.getContent());
            assertNull(inboxContentSchemaData.getMetadata());
        }
    }

    @Test
    public void constructor_handlesException_whenMetadataIsNotJSONObject() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            JSONObject contentObject = new JSONObject();
            contentObject.put("heading", "My Inbox");
            schemaData.put(CONTENT, contentObject);
            schemaData.put(METADATA, "invalidMetadata");

            // test
            InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertNotNull(inboxContentSchemaData.getContent());
            assertTrue(inboxContentSchemaData.getContent() instanceof Map);
            assertEquals("My Inbox", ((Map) inboxContentSchemaData.getContent()).get("heading"));
            assertNull(inboxContentSchemaData.getMetadata());
        }
    }

    @Test
    public void constructor_handlesException_whenBothContentAndMetadataAreMissing()
            throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();

            // test
            InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertNull(inboxContentSchemaData.getContent());
            assertNull(inboxContentSchemaData.getMetadata());
        }
    }

    @Test
    public void constructor_handlesEmptyJSONObject() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();

            // test
            InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertNull(inboxContentSchemaData.getContent());
            assertNull(inboxContentSchemaData.getMetadata());
        }
    }

    @Test
    public void constructor_setsContentWithComplexNestedStructure() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        JSONObject contentObject = new JSONObject();
        contentObject.put("heading", "My Inbox");
        contentObject.put("capacity", 10);

        JSONObject layoutObject = new JSONObject();
        layoutObject.put("orientation", "vertical");
        contentObject.put("layout", layoutObject);

        JSONObject emptyStateSettings = new JSONObject();
        emptyStateSettings.put("message", "No messages");
        contentObject.put("emptyStateSettings", emptyStateSettings);

        schemaData.put(CONTENT, contentObject);

        JSONObject metadataObject = new JSONObject();
        metadataObject.put("metaKey", "metaValue");
        schemaData.put(METADATA, metadataObject);

        // test
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // verify
        assertNotNull(inboxContentSchemaData.getContent());
        Map<String, Object> content = inboxContentSchemaData.getContent();
        assertEquals("My Inbox", content.get("heading"));
        assertEquals(10, content.get("capacity"));
        assertTrue(content.get("layout") instanceof Map);
        assertEquals("vertical", ((Map) content.get("layout")).get("orientation"));
        assertTrue(content.get("emptyStateSettings") instanceof Map);
        assertEquals("No messages", ((Map) content.get("emptyStateSettings")).get("message"));

        assertNotNull(inboxContentSchemaData.getMetadata());
        assertEquals("metaValue", inboxContentSchemaData.getMetadata().get("metaKey"));
    }

    @Test
    public void getContent_returnsCorrectMap() throws JSONException {
        // setup
        JSONObject schemaData = createValidInboxSchemaObject();
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // test
        Map<String, Object> content = inboxContentSchemaData.getContent();

        // verify
        assertNotNull(content);
        assertEquals("My Inbox", content.get("heading"));
        assertEquals(10, content.get("capacity"));
    }

    @Test
    public void getMetadata_returnsCorrectMap() throws JSONException {
        // setup
        JSONObject schemaData = createValidInboxSchemaObject();
        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // test
        Map<String, Object> metadata = inboxContentSchemaData.getMetadata();

        // verify
        assertNotNull(metadata);
        assertEquals("metaValue", metadata.get("metaKey"));
    }

    @Test
    public void getContent_returnsNull_whenContentWasNotSet() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        JSONObject metadataObject = new JSONObject();
        metadataObject.put("metaKey", "metaValue");
        schemaData.put(METADATA, metadataObject);

        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // test
        Map<String, Object> content = inboxContentSchemaData.getContent();

        // verify
        assertNull(content);
    }

    @Test
    public void getMetadata_returnsNull_whenMetadataWasNotSet() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        JSONObject contentObject = new JSONObject();
        contentObject.put("heading", "My Inbox");
        schemaData.put(CONTENT, contentObject);

        InboxContentSchemaData inboxContentSchemaData = new InboxContentSchemaData(schemaData);

        // test
        Map<String, Object> metadata = inboxContentSchemaData.getMetadata();

        // verify
        assertNull(metadata);
    }

    private JSONObject createValidInboxSchemaObject() throws JSONException {
        JSONObject schemaData = new JSONObject();

        JSONObject contentObject = new JSONObject();
        contentObject.put("heading", "My Inbox");
        contentObject.put("capacity", 10);

        JSONObject metadataObject = new JSONObject();
        metadataObject.put("metaKey", "metaValue");

        schemaData.put(CONTENT, contentObject);
        schemaData.put(METADATA, metadataObject);

        return schemaData;
    }
}
