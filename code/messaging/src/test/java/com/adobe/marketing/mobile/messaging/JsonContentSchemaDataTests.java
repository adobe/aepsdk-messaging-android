package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.FORMAT;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.services.Log;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class JsonContentSchemaDataTests {

    @Test
    public void constructor_setsDefaultContentType_whenContentTypeIsMissing() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));

        //test
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, jsonContentSchemaData.getFormat());
        assertEquals("value", jsonContentSchemaData.getJsonObjectContent().get("key"));
    }

    @Test
    public void constructor_setsContentType_whenContentTypeIsApplicationJson() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

        //test
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, jsonContentSchemaData.getFormat());
        assertEquals("value", jsonContentSchemaData.getJsonObjectContent().get("key"));
    }

    @Test
    public void constructor_setsContentType_whenContentTypeIsNotApplicationJson() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.TEXT_HTML);

        //test
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //verify
        assertEquals(ContentType.TEXT_HTML, jsonContentSchemaData.getFormat());
        assertEquals("value", jsonContentSchemaData.getJsonObjectContent().get("key"));
    }

    @Test
    public void constructor_setsContentAsList_whenContentIsJsonArray() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONArray().put(new JSONObject().put("key", "value")));
        schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

        //test
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, jsonContentSchemaData.getFormat());
        assertEquals("value", jsonContentSchemaData.getJsonArrayContent().get(0).get("key"));
    }

    @Test
    public void constructor_logsException_whenContentIsNotJson() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            //setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT, "content");
            schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

            //test
            JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

            //verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertNull(jsonContentSchemaData.getJsonObjectContent());
            assertNull(jsonContentSchemaData.getJsonArrayContent());
        }
    }

    @Test
    public void constructor_logsException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            //setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(FORMAT, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

            //test
            JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

            //verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertNull(jsonContentSchemaData.getJsonObjectContent());
            assertNull(jsonContentSchemaData.getJsonArrayContent());
        }
    }

    @Test
    public void getContent_returnsNull_whenContentIsNull() {
        //setup
        JSONObject schemaData = new JSONObject();
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //test
        Object result = jsonContentSchemaData.getContent();

        //verify
        assertNull(result);
    }

    @Test
    public void getContent_returnsNonNullObject_whenContentIsJsonObject() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //test
        Object result = jsonContentSchemaData.getContent();

        //verify
        assertNotNull(result);
        assertEquals("value", ((Map) result).get("key"));
    }

    @Test
    public void getContent_returnsNonNullObject_whenContentIsJsonArray() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONArray().put(new JSONObject().put("key", "value")));
        JsonContentSchemaData jsonContentSchemaData = new JsonContentSchemaData(schemaData);

        //test
        Object result = jsonContentSchemaData.getContent();

        //verify
        assertNotNull(result);
        assertEquals("value", ((List<Map>) result).get(0).get("key"));
    }
}
