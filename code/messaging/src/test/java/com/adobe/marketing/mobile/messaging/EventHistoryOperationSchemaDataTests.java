/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventHistoryOperationSchemaDataTests {

    @Test
    public void constructor_setsAllFieldsCorrectly_whenValidJsonObjectProvided()
            throws JSONException {
        JSONObject schemaData = new JSONObject();
        schemaData.put(MessagingTestConstants.ConsequenceDetailDataKeys.OPERATION, "insert");
        JSONObject contentObject = new JSONObject();
        contentObject.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.EVENT_TYPE, "customEvent");
        contentObject.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.ACTIVITY_ID, "activity123");
        schemaData.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, contentObject);

        Map<String, Object> expectedContent = new HashMap<>();
        expectedContent.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.EVENT_TYPE, "customEvent");
        expectedContent.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.ACTIVITY_ID, "activity123");

        try (MockedStatic<JSONUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JSONUtils.class);
                MockedStatic<DataReader> dataReaderMockedStatic =
                        Mockito.mockStatic(DataReader.class)) {

            jsonUtilsMockedStatic
                    .when(() -> JSONUtils.toMap(any(JSONObject.class)))
                    .thenReturn(expectedContent);
            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            expectedContent,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .EVENT_TYPE,
                                            null))
                    .thenReturn("customEvent");
            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            expectedContent,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .ACTIVITY_ID,
                                            null))
                    .thenReturn("activity123");

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            assertEquals("insert", eventHistoryOperationSchemaData.getOperation());
            assertEquals(expectedContent, eventHistoryOperationSchemaData.getContent());
            assertEquals("customEvent", eventHistoryOperationSchemaData.getEventType());
            assertEquals("activity123", eventHistoryOperationSchemaData.getActivityId());
        }
    }

    @Test
    public void constructor_handlesJSONException_whenOperationIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            JSONObject schemaData = new JSONObject();
            schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.CONTENT, new JSONObject());

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    eq(MessagingConstants.LOG_TAG),
                                    anyString(),
                                    anyString(),
                                    anyString()));
            assertNull(eventHistoryOperationSchemaData.getOperation());
            assertNull(eventHistoryOperationSchemaData.getContent());
        }
    }

    @Test
    public void constructor_handlesJSONException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            JSONObject schemaData = new JSONObject();
            schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.OPERATION, "deleteEvent");

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    eq(MessagingConstants.LOG_TAG),
                                    anyString(),
                                    anyString(),
                                    anyString()));
            assertEquals("deleteEvent", eventHistoryOperationSchemaData.getOperation());
            assertNull(eventHistoryOperationSchemaData.getContent());
        }
    }

    @Test
    public void constructor_handlesJSONException_whenNullJsonObjectProvided() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(null);

            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    eq(MessagingConstants.LOG_TAG),
                                    anyString(),
                                    anyString(),
                                    anyString()));
            assertNull(eventHistoryOperationSchemaData.getOperation());
            assertNull(eventHistoryOperationSchemaData.getContent());
        }
    }

    @Test
    public void getEventType_returnsNull_whenContentIsNull() {
        try (MockedStatic<DataReader> dataReaderMockedStatic =
                Mockito.mockStatic(DataReader.class)) {
            JSONObject schemaData = new JSONObject();
            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            null,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .EVENT_TYPE,
                                            null))
                    .thenReturn(null);

            assertNull(eventHistoryOperationSchemaData.getEventType());
        }
    }

    @Test
    public void getActivityId_returnsNull_whenContentIsNull() {
        try (MockedStatic<DataReader> dataReaderMockedStatic =
                Mockito.mockStatic(DataReader.class)) {
            JSONObject schemaData = new JSONObject();
            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            null,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .ACTIVITY_ID,
                                            null))
                    .thenReturn(null);

            assertNull(eventHistoryOperationSchemaData.getActivityId());
        }
    }

    @Test
    public void getEventType_returnsNull_whenEventTypeNotInContent() throws JSONException {
        JSONObject schemaData = new JSONObject();
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.OPERATION, "insert");
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.CONTENT, new JSONObject());

        Map<String, Object> content = new HashMap<>();

        try (MockedStatic<JSONUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JSONUtils.class);
                MockedStatic<DataReader> dataReaderMockedStatic =
                        Mockito.mockStatic(DataReader.class)) {

            jsonUtilsMockedStatic
                    .when(() -> JSONUtils.toMap(any(JSONObject.class)))
                    .thenReturn(content);
            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            content,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .EVENT_TYPE,
                                            null))
                    .thenReturn(null);

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            assertNull(eventHistoryOperationSchemaData.getEventType());
        }
    }

    @Test
    public void getActivityId_returnsNull_whenActivityIdNotInContent() throws JSONException {
        JSONObject schemaData = new JSONObject();
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.OPERATION, "insert");
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.CONTENT, new JSONObject());

        Map<String, Object> content = new HashMap<>();

        try (MockedStatic<JSONUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JSONUtils.class);
                MockedStatic<DataReader> dataReaderMockedStatic =
                        Mockito.mockStatic(DataReader.class)) {

            jsonUtilsMockedStatic
                    .when(() -> JSONUtils.toMap(any(JSONObject.class)))
                    .thenReturn(content);
            dataReaderMockedStatic
                    .when(
                            () ->
                                    DataReader.optString(
                                            content,
                                            MessagingTestConstants.ConsequenceDetailDataKeys
                                                    .ACTIVITY_ID,
                                            null))
                    .thenReturn(null);

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            assertNull(eventHistoryOperationSchemaData.getActivityId());
        }
    }

    @Test
    public void constructor_setsOperationOnly_whenContentIsEmpty() throws JSONException {
        JSONObject schemaData = new JSONObject();
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.OPERATION, "clearEvents");
        schemaData.put(MessagingConstants.ConsequenceDetailDataKeys.CONTENT, new JSONObject());

        Map<String, Object> emptyContent = new HashMap<>();

        try (MockedStatic<JSONUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JSONUtils.class)) {
            jsonUtilsMockedStatic
                    .when(() -> JSONUtils.toMap(any(JSONObject.class)))
                    .thenReturn(emptyContent);

            EventHistoryOperationSchemaData eventHistoryOperationSchemaData =
                    new EventHistoryOperationSchemaData(schemaData);

            assertEquals("clearEvents", eventHistoryOperationSchemaData.getOperation());
            assertEquals(emptyContent, eventHistoryOperationSchemaData.getContent());
        }
    }
}
