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

package com.adobe.marketing.mobile.messaging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.TimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedItemTests {
    // Mocks
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    DeviceInforming mockDeviceInfoService;

    private static final int SECONDS_IN_A_DAY = 86400;
    private static final String FEED_ITEM_CACHE = "feedItemTestCache";
    private static final String TITLE = "testTile";
    private static final String BODY = "testBody";
    private static final String IMAGE_URL = "testImageUrl";
    private static final String ACTION_URL = "testActionUrl";
    private static final String ACTION_TITLE = "testActionTitle";
    private final Map<String, Object> metaMap = new HashMap<String, Object>() {
        {
            put("stringKey", "value");
            put("key2", true);
            put("key3", 1000.1111);
        }
    };
    private File cacheDir;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        cacheDir = new File("cache");
        cacheDir.mkdirs();
        cacheDir.setWritable(true);
    }

    @After
    public void tearDown() {
        if (cacheDir.exists()) {
            cacheDir.delete();
        }
    }

    private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
        CacheService cacheService = ServiceProvider.getInstance().getCacheService();
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
            testRunnable.run();
        }
    }

    @Test
    public void testCreateFeedItem_AllParametersPresent() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, BODY, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.title);
        assertEquals(BODY, feedItem.body);
        assertEquals(IMAGE_URL, feedItem.imageUrl);
        assertEquals(ACTION_URL, feedItem.actionUrl);
        assertEquals(ACTION_TITLE, feedItem.actionTitle);
        assertEquals(publishedDate, feedItem.publishedDate);
        assertEquals(expiryDate, feedItem.expiryDate);
        assertEquals(metaMap, feedItem.meta);
    }

    @Test
    public void testCreateFeedItem_RequiredParametersOnly() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, BODY, null, null, null, publishedDate, expiryDate, null);

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.title);
        assertEquals(BODY, feedItem.body);
        assertEquals(null, feedItem.imageUrl);
        assertEquals(null, feedItem.actionUrl);
        assertEquals(null, feedItem.actionTitle);
        assertEquals(publishedDate, feedItem.publishedDate);
        assertEquals(expiryDate, feedItem.expiryDate);
        assertEquals(null, feedItem.meta);
    }

    @Test
    public void testCreateFeedItem_NoActionTitle() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, BODY, IMAGE_URL, ACTION_URL, null, publishedDate, expiryDate, metaMap);

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.title);
        assertEquals(BODY, feedItem.body);
        assertEquals(IMAGE_URL, feedItem.imageUrl);
        assertEquals(null, feedItem.actionUrl);
        assertEquals(null, feedItem.actionTitle);
        assertEquals(publishedDate, feedItem.publishedDate);
        assertEquals(expiryDate, feedItem.expiryDate);
        assertEquals(metaMap, feedItem.meta);
    }

    @Test
    public void testCreateFeedItem_NoActionUrl() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, BODY, IMAGE_URL, null, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.title);
        assertEquals(BODY, feedItem.body);
        assertEquals(IMAGE_URL, feedItem.imageUrl);
        assertEquals(null, feedItem.actionUrl);
        assertEquals(ACTION_TITLE, feedItem.actionTitle);
        assertEquals(publishedDate, feedItem.publishedDate);
        assertEquals(expiryDate, feedItem.expiryDate);
        assertEquals(metaMap, feedItem.meta);
    }

    @Test
    public void testCreateFeedItem_NoTitle() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(null, BODY, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_NoBody() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, null, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_InvalidPublishedDate() {
        // setup
        long publishedDate = 0;
        long expiryDate = TimeUtils.getUnixTimeInSeconds() + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, null, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_InvalidExpiryDate() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = 0;

        // test
        FeedItem feedItem = FeedItem.create(TITLE, null, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testFeedItemIsSerializable() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;
        FeedItem feedItem = FeedItem.create(TITLE, BODY, IMAGE_URL, ACTION_URL, ACTION_TITLE, publishedDate, expiryDate, metaMap);
        setupServiceProviderMockAndRunTest(() -> {
            // write to cache
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(feedItem);
                objectOutputStream.flush();
                objectOutputStream.close();
            } catch (IOException ioException) {
                fail(ioException.getMessage());
            }

            InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            CacheEntry feedItemCacheEntry = new CacheEntry(inputStream, CacheExpiry.never(), null);

            // test
            CacheService cacheService = ServiceProvider.getInstance().getCacheService();
            cacheService.set(FEED_ITEM_CACHE, "feedItem", feedItemCacheEntry);
            CacheResult feedItemCacheResult = cacheService.get(FEED_ITEM_CACHE, "feedItem");
            ObjectInputStream objectInputStream;
            FeedItem cachedFeedItem = null;
            try {
                objectInputStream = new ObjectInputStream(feedItemCacheResult.getData());
                cachedFeedItem = (FeedItem) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException | ClassNotFoundException exception) {
                fail(exception.getMessage());
            }


            // verify
            assertNotNull(cachedFeedItem);
            assertEquals(TITLE, cachedFeedItem.title);
            assertEquals(BODY, cachedFeedItem.body);
            assertEquals(IMAGE_URL, cachedFeedItem.imageUrl);
            assertEquals(ACTION_URL, cachedFeedItem.actionUrl);
            assertEquals(ACTION_TITLE, cachedFeedItem.actionTitle);
            assertEquals(publishedDate, cachedFeedItem.publishedDate);
            assertEquals(expiryDate, cachedFeedItem.expiryDate);
            assertEquals(metaMap, cachedFeedItem.meta);
        });
    }
}
