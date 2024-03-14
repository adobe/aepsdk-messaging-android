package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.METADATA;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.MessageFeedKeys.ACTION_TITLE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.MessageFeedKeys.ACTION_URL;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.MessageFeedKeys.BODY;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.MessageFeedKeys.IMAGE_URL;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.MessageFeedKeys.TITLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.services.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class FeedItemSchemaDataTests {

    @Test
    public void constructor_setsAllFieldsCorrectly_whenContentTypeIsApplicationJson() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);

        // verify
        assertEquals(ContentType.APPLICATION_JSON, feedItemSchemaData.getContentType());
        assertTrue(feedItemSchemaData.getContent() instanceof Map);
        assertEquals("value", ((Map) feedItemSchemaData.getContent()).get("key"));
        assertEquals(123456789, feedItemSchemaData.getPublishedDate());
        assertEquals(987654321, feedItemSchemaData.getExpiryDate());
        assertEquals("metaValue", feedItemSchemaData.getMeta().get("metaKey"));
    }

    @Test
    public void constructor_setsAllFieldsCorrectly_whenContentTypeIsString() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingConstants.ContentTypes.TEXT_PLAIN);
        schemaData.put(CONTENT, "content");
        schemaData.put(PUBLISHED_DATE, 123456789);
        schemaData.put(EXPIRY_DATE, 987654321);
        schemaData.put(METADATA, new JSONObject().put("metaKey", "metaValue"));

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);

        // verify
        assertEquals(ContentType.TEXT_PLAIN, feedItemSchemaData.getContentType());
        assertTrue(feedItemSchemaData.getContent() instanceof String);
        assertEquals("content", feedItemSchemaData.getContent());
        assertEquals(123456789, feedItemSchemaData.getPublishedDate());
        assertEquals(987654321, feedItemSchemaData.getExpiryDate());
        assertEquals("metaValue", feedItemSchemaData.getMeta().get("metaKey"));
    }

    @Test
    public void constructor_handlesJSONException_whenBadJsonObjectInput() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
            schemaData.put(CONTENT, "invalidJson");

            // test
            FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);

            // verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, feedItemSchemaData.getContentType());
            assertNull(feedItemSchemaData.getContent());
            assertEquals(0, feedItemSchemaData.getPublishedDate());
            assertEquals(0, feedItemSchemaData.getExpiryDate());
            assertNull(feedItemSchemaData.getMeta());
        }
    }

    @Test
    public void constructor_handlesJSONException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

            // test
            FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);

            // verify
            logMockedStatic.verify(() -> Log.trace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, feedItemSchemaData.getContentType());
            assertNull(feedItemSchemaData.getContent());
            assertEquals(0, feedItemSchemaData.getPublishedDate());
            assertEquals(0, feedItemSchemaData.getExpiryDate());
            assertNull(feedItemSchemaData.getMeta());
        }
    }

    @Test
    public void constructor_setsAllFieldsCorrectly_whenPublishedDateExpiryDateMetaAreMissing() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);

        // verify
        assertEquals(ContentType.APPLICATION_JSON, feedItemSchemaData.getContentType());
        assertTrue(feedItemSchemaData.getContent() instanceof Map);
        assertEquals("value", ((Map) feedItemSchemaData.getContent()).get("key"));
        assertEquals(0, feedItemSchemaData.getPublishedDate());
        assertEquals(0, feedItemSchemaData.getExpiryDate());
        assertNull(feedItemSchemaData.getMeta());
    }

    @Test
    public void getFeedItem_returnsFeedItem_whenContentTypeIsApplicationJson() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, new JSONObject()
                .put(TITLE, "title")
                .put(BODY, "body")
                .put(IMAGE_URL, "imageUrl")
                .put(ACTION_URL, "actionUrl")
                .put(ACTION_TITLE, "actionTitle"));

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);
        FeedItem feedItem = feedItemSchemaData.getFeedItem();

        // verify
        assertEquals("title", feedItem.getTitle());
        assertEquals("body", feedItem.getBody());
        assertEquals("imageUrl", feedItem.getImageUrl());
        assertEquals("actionUrl", feedItem.getActionUrl());
        assertEquals("actionTitle", feedItem.getActionTitle());
    }

    @Test
    public void getFeedItem_returnsNull_whenContentTypeIsNotApplicationJson() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingConstants.ContentTypes.TEXT_PLAIN);
        schemaData.put(CONTENT, "content");

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);
        FeedItem feedItem = feedItemSchemaData.getFeedItem();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void getFeedItem_returnsNull_whenContentIsNotMap() throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, "content");

        // test
        FeedItemSchemaData feedItemSchemaData = new FeedItemSchemaData(schemaData);
        FeedItem feedItem = feedItemSchemaData.getFeedItem();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void getEmpty_returnsEmptyFeedItemSchemaData() {
        // test
        FeedItemSchemaData feedItemSchemaData = FeedItemSchemaData.getEmpty();

        // verify
        assertEquals(ContentType.UNKNOWN, feedItemSchemaData.getContentType());
        assertNull(feedItemSchemaData.getContent());
        assertEquals(0, feedItemSchemaData.getPublishedDate());
        assertEquals(0, feedItemSchemaData.getExpiryDate());
        assertNull(feedItemSchemaData.getMeta());
    }
}
