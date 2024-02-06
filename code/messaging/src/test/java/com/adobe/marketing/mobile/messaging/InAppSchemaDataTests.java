package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.METADATA;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.WEB_PARAMETERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


import com.adobe.marketing.mobile.services.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class InAppSchemaDataTests {
    @Test
    public void constructor_setsFieldsCorrectly_whenContentIsJsonObject() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
        schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
        schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
        schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));

        //test
        InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, inAppSchemaData.getContentType());
        assertEquals("value", ((Map) inAppSchemaData.getContent()).get("key"));
        assertEquals(123456789, inAppSchemaData.getPublishedDate());
        assertEquals(987654321, inAppSchemaData.getExpiryDate());
        assertEquals("metaValue", inAppSchemaData.getMeta().get("metaKey"));
        assertEquals("mobileValue", inAppSchemaData.getMobileParameters().get("mobileKey"));
        assertEquals("webValue", inAppSchemaData.getWebParameters().get("webKey"));
        assertEquals(1, inAppSchemaData.getRemoteAssets().size());
        assertEquals("https://somedomain.com/someimage.jpg", inAppSchemaData.getRemoteAssets().get(0));
    }

    @Test
    public void constructor_setsFieldsCorrectly_whenContentIsJsonArray() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
        JSONArray contentArray = new JSONArray();
        contentArray.put(new JSONObject().put("key1", "value1"));
        contentArray.put(new JSONObject().put("key2", "value2"));
        schemaData.put(CONTENT, contentArray);
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
        schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
        schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
        schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));

        //test
        InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, inAppSchemaData.getContentType());
        assertEquals(2, ((List) inAppSchemaData.getContent()).size());
        assertEquals("value1", ((Map) ((List) inAppSchemaData.getContent()).get(0)).get("key1"));
        assertEquals("value2", ((Map) ((List) inAppSchemaData.getContent()).get(1)).get("key2"));
        assertEquals(123456789, inAppSchemaData.getPublishedDate());
        assertEquals(987654321, inAppSchemaData.getExpiryDate());
        assertEquals("metaValue", inAppSchemaData.getMeta().get("metaKey"));
        assertEquals("mobileValue", inAppSchemaData.getMobileParameters().get("mobileKey"));
        assertEquals("webValue", inAppSchemaData.getWebParameters().get("webKey"));
        assertEquals(1, inAppSchemaData.getRemoteAssets().size());
        assertEquals("https://somedomain.com/someimage.jpg", inAppSchemaData.getRemoteAssets().get(0));
    }

    @Test
    public void constructor_setsFieldsCorrectly_whenContentTypeIsNotApplicationJson() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.TEXT_HTML);
        schemaData.put(CONTENT, "content");
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
        schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
        schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
        schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));

        //test
        InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

        //verify
        assertEquals(ContentType.TEXT_HTML, inAppSchemaData.getContentType());
        assertEquals("content", inAppSchemaData.getContent());
        assertEquals(123456789, inAppSchemaData.getPublishedDate());
        assertEquals(987654321, inAppSchemaData.getExpiryDate());
        assertEquals("metaValue", inAppSchemaData.getMeta().get("metaKey"));
        assertEquals("mobileValue", inAppSchemaData.getMobileParameters().get("mobileKey"));
        assertEquals("webValue", inAppSchemaData.getWebParameters().get("webKey"));
        assertEquals(1, inAppSchemaData.getRemoteAssets().size());
        assertEquals("https://somedomain.com/someimage.jpg", inAppSchemaData.getRemoteAssets().get(0));
    }

    @Test
    public void constructor_returns_whenContentTypeIsMissing() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
        schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
        schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
        schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));
        //test
        InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

        //verify
        assertEquals(ContentType.UNKNOWN, inAppSchemaData.getContentType());
        assertNull("content", inAppSchemaData.getContent());
        assertEquals(0, inAppSchemaData.getPublishedDate());
        assertEquals(0, inAppSchemaData.getExpiryDate());
        assertNull(inAppSchemaData.getMeta());
        assertNull(inAppSchemaData.getMobileParameters());
        assertNull(inAppSchemaData.getWebParameters());
        assertNull(inAppSchemaData.getRemoteAssets());
    }

    @Test
    public void constructor_logsException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            //setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
            schemaData.put(PUBLISHED_DATE, 123456789);
            schemaData.put(EXPIRY_DATE, 987654321);
            schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
            schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
            schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
            schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));
            //test
            InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

            //verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, inAppSchemaData.getContentType());
            assertNull("content", inAppSchemaData.getContent());
            assertEquals(0, inAppSchemaData.getPublishedDate());
            assertEquals(0, inAppSchemaData.getExpiryDate());
            assertNull(inAppSchemaData.getMeta());
            assertNull(inAppSchemaData.getMobileParameters());
            assertNull(inAppSchemaData.getWebParameters());
            assertNull(inAppSchemaData.getRemoteAssets());
        }
    }

    @Test
    public void constructor_logsException_whenContentTypeIsJsonAndContentIsNot() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            //setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
            schemaData.put(CONTENT, "invalidJson");
            schemaData.put(PUBLISHED_DATE, 123456789);
            schemaData.put(EXPIRY_DATE, 987654321);
            schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));
            schemaData.put(MOBILE_PARAMETERS, new JSONObject().put("mobileKey", "mobileValue"));
            schemaData.put(WEB_PARAMETERS, new JSONObject().put("webKey", "webValue"));
            schemaData.put(REMOTE_ASSETS, new JSONArray().put("https://somedomain.com/someimage.jpg"));
            //test
            InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

            //verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, inAppSchemaData.getContentType());
            assertNull("content", inAppSchemaData.getContent());
            assertEquals(0, inAppSchemaData.getPublishedDate());
            assertEquals(0, inAppSchemaData.getExpiryDate());
            assertNull(inAppSchemaData.getMeta());
            assertNull(inAppSchemaData.getMobileParameters());
            assertNull(inAppSchemaData.getWebParameters());
            assertNull(inAppSchemaData.getRemoteAssets());
        }
    }

    @Test
    public void constructor_setsFieldsCorrectly_whenOnlyContentTypeAndContentArePresent() throws JSONException {
        //setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));

        //test
        InAppSchemaData inAppSchemaData = new InAppSchemaData(schemaData);

        //verify
        assertEquals(ContentType.APPLICATION_JSON, inAppSchemaData.getContentType());
        assertEquals("value", ((Map) inAppSchemaData.getContent()).get("key"));
        assertEquals(0, inAppSchemaData.getPublishedDate());
        assertEquals(0, inAppSchemaData.getExpiryDate());
        assertNull(inAppSchemaData.getMeta());
        assertNull(inAppSchemaData.getMobileParameters());
        assertNull(inAppSchemaData.getWebParameters());
        assertNull(inAppSchemaData.getRemoteAssets());
    }
}
