/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;

import com.adobe.marketing.mobile.messaging.Proposition;
import com.adobe.marketing.mobile.messaging.MessagingTestConstants;
import com.adobe.marketing.mobile.messaging.MessagingTestUtils;
import com.adobe.marketing.mobile.messaging.Surface;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingTests {
    @Mock
    Intent mockIntent;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    DeviceInforming mockDeviceInfoService;

    private void runWithMockedMobileCore(final ArgumentCaptor<Event> eventArgumentCaptor,
                                         final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackWithErrorArgumentCaptor,
                                         final ArgumentCaptor<ExtensionErrorCallback<ExtensionError>> extensionErrorCallbackArgumentCaptor,
                                         final Runnable testRunnable) {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class); MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            mobileCoreMockedStatic.when(() -> MobileCore.registerExtension(any(), extensionErrorCallbackArgumentCaptor != null ? extensionErrorCallbackArgumentCaptor.capture() : any(ExtensionErrorCallback.class))).thenCallRealMethod();
            mobileCoreMockedStatic.when(() -> MobileCore.dispatchEventWithResponseCallback(eventArgumentCaptor.capture(), anyLong(), callbackWithErrorArgumentCaptor != null ? callbackWithErrorArgumentCaptor.capture() : any(AdobeCallbackWithError.class))).thenCallRealMethod();
            mobileCoreMockedStatic.when(() -> MobileCore.dispatchEvent(eventArgumentCaptor.capture())).thenCallRealMethod();

            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("com.adobe.marketing.mobile.messaging.test");
            testRunnable.run();
        }
    }

    @After
    public void tearDown() {
        reset(mockIntent);
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
    }

    // ========================================================================================
    // extensionVersion
    // ========================================================================================

    @Test
    public void test_extensionVersionAPI() {
        // test
        String extensionVersion = Messaging.extensionVersion();
        Assert.assertEquals("The Extension version API returns the correct value", MessagingTestConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    // ========================================================================================
    // registerExtension
    // ========================================================================================

    @Test
    public void test_registerExtensionAPI() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback<ExtensionError>> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
        runWithMockedMobileCore(eventCaptor, null, callbackCaptor, () -> {
            // test
            Messaging.registerExtension();

            // The monitor extension should register with core
            MobileCore.registerExtension(ArgumentMatchers.eq(MessagingExtension.class), callbackCaptor.capture());

            // verify the callback
            ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getAllValues().get(0);
            Assert.assertNotNull("The extension callback should not be null", extensionErrorCallback);

            // should not crash on calling the callback
            extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
        });
    }

    // ========================================================================================
    // addPushTrackingDetails
    // ========================================================================================
    @Test
    public void test_addPushTrackingDetails_WhenParamsAreNull() {
        // test
        boolean done = Messaging.addPushTrackingDetails(null, null, null);

        // verify
        Assert.assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetails() {
        String mockMessageId = "mockMessageId";
        String mockXDMData = "mockXDMData";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, mockXDMData);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockXDMData);
    }

    @Test
    public void test_addPushTrackingDetailsNoXdmData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, null);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(0)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, "");
    }

    @Test
    public void test_addPushTrackingDetailsWithEmptyData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetailsWithNullData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = null;

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetailsWithEmptyMessageId() {
        String mockMessageId = "";
        String mockXDMData = "mockXDMData";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, mockXDMData);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertFalse(done);
    }

    // ========================================================================================
    // handleNotificationResponse
    // ========================================================================================
    @Test
    public void test_handleNotificationResponse_WhenParamsAreNull() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            // test
            Messaging.handleNotificationResponse(null, false, null);

            // verify
            verifyNoInteractions(MobileCore.class);
        });
    }

    @Test
    public void test_handleNotificationResponse() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            String mockActionId = "mockActionId";
            String mockXdm = "mockXdm";

            when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());
            MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

            // verify event
            Event event = eventCaptor.getValue();
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(eventData.get(TRACK_INFO_KEY_ACTION_ID), mockActionId);
        });
    }

    @Test
    public void test_handleNotificationResponseNoXdmData() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            String mockActionId = "mockActionId";
            String messageId = "messageId";

            when(mockIntent.getStringExtra(ArgumentMatchers.contains("messageId"))).thenReturn(messageId);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());

            // verify no event captured
            assertEquals(0, eventCaptor.getAllValues().size());
        });
    }

    @Test
    public void test_handleNotificationResponseEventDispatchError() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            String mockActionId = "mockActionId";
            String mockXdm = "mockXdm";

            when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());

            MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture());

            // verify event
            Event event = eventCaptor.getAllValues().get(0);
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(eventData.get(TRACK_INFO_KEY_ACTION_ID), mockActionId);
            // no exception should occur when triggering unexpected error callback
            callbackCaptor.getAllValues().get(0).fail(AdobeError.UNEXPECTED_ERROR);
        });
    }

    @Test
    public void test_handleNotificationResponseWithEmptyMessageId() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            String mockActionId = "mockActionId";
            String messageId = "";

            when(mockIntent.getStringExtra(anyString())).thenReturn(messageId);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());
            verifyNoInteractions(MobileCore.class);
        });
    }

    @Test
    public void test_handleNotificationResponseWithEmptyAction() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            String mockActionId = "";
            String messageId = "mockXdm";

            when(mockIntent.getStringExtra(anyString())).thenReturn(messageId);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());

            // verify event
            Event event = eventCaptor.getAllValues().get(0);
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals("", mockActionId);
        });
    }

    // ========================================================================================
    // refreshInAppMessage
    // ========================================================================================
    @Test
    public void test_refreshInAppMessage() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {

            // test
            Messaging.refreshInAppMessages();

            // verify
            MobileCore.dispatchEvent(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getAllValues().get(0);
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(true, DataReader.optBoolean(eventData, MessagingTestConstants.EventDataKeys.Messaging.REFRESH_MESSAGES, false));
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT, event.getName());
        });
    }

    // ========================================================================================
    // getPropositionsForSurfaces
    // ========================================================================================
    @Test
    public void testGetPropositionsForSurfacePaths_validSurface() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            Surface feedSurface = new Surface("apifeed");
            Surface codeBasedSurface = new Surface("cbe");
            surfacePaths.add(feedSurface);
            surfacePaths.add(codeBasedSurface);

            // test
            CountDownLatch latch = new CountDownLatch(1);
            final AdobeError[] responseError = new AdobeError[1];
            final Map<Surface, List<Proposition>>[] responseMapForSurface = new Map[]{null};
            Messaging.getPropositionsForSurfaces(surfacePaths, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError[0] = adobeError;
                    latch.countDown();
                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                    responseMapForSurface[0] = surfaceListMap;
                    latch.countDown();
                }
            });

            // verify dispatched event
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();
            Assert.assertNotNull(event);
            Assert.assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            Assert.assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
            final Map<String, Object> eventData = event.getEventData();
            Assert.assertTrue((Boolean) eventData.get(MessagingTestConstants.EventDataKeys.Messaging.GET_PROPOSITIONS));
            List<Map<String, Object>> flattenedSurfaces = new ArrayList<Map<String, Object>>() {{
                add(new HashMap<String, Object>() {{
                    put("uri", "mobileapp://com.adobe.marketing.mobile.messaging.test/apifeed");
                }});
                add(new HashMap<String, Object>() {{
                    put("uri", "mobileapp://com.adobe.marketing.mobile.messaging.test/cbe");
                }});
            }};
            Assert.assertEquals(flattenedSurfaces, eventData.get(MessagingTestConstants.EventDataKeys.Messaging.SURFACES));

            // verify callback response
            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            final Map<String, Object> feedPropositionData = MessagingTestUtils.getMapFromFile("feedProposition.json");
            final Map<String, Object> codeBasedPropositionData = MessagingTestUtils.getMapFromFile("codeBasedProposition.json");
            propositionsList.add(feedPropositionData);
            propositionsList.add(codeBasedPropositionData);

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put(MessagingTestConstants.EventDataKeys.Messaging.PROPOSITIONS, propositionsList);
            final Event responseEvent = new Event.Builder(
                    MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                    MessagingTestConstants.EventType.MESSAGING,
                    MessagingTestConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            try {
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail("getPropositionsForSurfaces callback not called");
            }
            Assert.assertNull(responseError[0]);
            Assert.assertNotNull(responseMapForSurface[0]);

            List<Proposition> feedPropositions = responseMapForSurface[0].get(feedSurface);
            Assert.assertNotNull(feedPropositions);
            Assert.assertEquals(1, feedPropositions.size());
            Assert.assertEquals("c2aa4a73-a534-44c2-baa4-a12980e5bb9d", feedPropositions.get(0).getUniqueId());
            Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/apifeed", feedPropositions.get(0).getScope());
            Assert.assertEquals(1, feedPropositions.get(0).getItems().size());

            List<Proposition> codeBasedPropositions = responseMapForSurface[0].get(codeBasedSurface);
            Assert.assertNotNull(codeBasedPropositions);
            Assert.assertEquals(1, codeBasedPropositions.size());
            Assert.assertEquals("d5072be7-5317-4ee4-b52b-1710ab60748f", codeBasedPropositions.get(0).getUniqueId());
            Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/cbe", codeBasedPropositions.get(0).getScope());
            Assert.assertEquals(1, codeBasedPropositions.get(0).getItems().size());
        });
    }

    @Test
    public void testGetPropositionsForSurfacePaths_nullCallback() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Messaging.getPropositionsForSurfaces(new ArrayList<Surface>() {{
                add(new Surface("apifeed"));
            }}, null);

            // verify no event dispatched
            mobileCoreMockedStatic.verifyNoInteractions();
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testGetPropositionsForSurfacePaths_nullSurfaces() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Messaging.getPropositionsForSurfaces(null, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {

                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {

                }
            });

            // verify no event dispatched
            mobileCoreMockedStatic.verifyNoInteractions();
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testGetPropositionsForSurfacePaths_emptySurfaces() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Messaging.getPropositionsForSurfaces(new ArrayList<>(), new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {

                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {

                }
            });

            // verify no event dispatched
            mobileCoreMockedStatic.verifyNoInteractions();
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testGetPropositionsForSurfacePaths_invalidSurfaces() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Messaging.getPropositionsForSurfaces(new ArrayList<Surface>() {{
                                                     add(new Surface(""));
                                                 }},
                    new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                        @Override
                        public void fail(AdobeError adobeError) {

                        }

                        @Override
                        public void call(Map<Surface, List<Proposition>> surfaceListMap) {

                        }
                    });

            // verify no event dispatched
            mobileCoreMockedStatic.verifyNoInteractions();
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testGetPropositionsForSurfacePaths_noEventDataInResponseEvent() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            Surface feedSurface = new Surface("apifeed");
            surfacePaths.add(feedSurface);

            // test
            CountDownLatch latch = new CountDownLatch(1);
            final AdobeError[] responseError = new AdobeError[1];
            final Map<Surface, List<Proposition>>[] responseMapForSurface = new Map[]{null};
            Messaging.getPropositionsForSurfaces(surfacePaths, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError[0] = adobeError;
                    latch.countDown();
                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                    responseMapForSurface[0] = surfaceListMap;
                    latch.countDown();
                }
            });

            // verify dispatched event
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();
            Assert.assertNotNull(event);
            Assert.assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            Assert.assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());

            // verify callback response
            final Map<String, Object> responseEventData = new HashMap<>();
            final Event responseEvent = new Event.Builder(
                    MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                    MessagingTestConstants.EventType.MESSAGING,
                    MessagingTestConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            try {
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail("getPropositionsForSurfaces callback not called");
            }
            Assert.assertNotNull(responseError[0]);
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError[0]);
            Assert.assertNull(responseMapForSurface[0]);

        });
    }

    @Test
    public void testGetPropositionsForSurfacePaths_responseEventDataContainsError() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            Surface feedSurface = new Surface("apifeed");
            surfacePaths.add(feedSurface);

            // test
            CountDownLatch latch = new CountDownLatch(1);
            final AdobeError[] responseError = new AdobeError[1];
            final Map<Surface, List<Proposition>>[] responseMapForSurface = new Map[]{null};
            Messaging.getPropositionsForSurfaces(surfacePaths, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError[0] = adobeError;
                    latch.countDown();
                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                    responseMapForSurface[0] = surfaceListMap;
                    latch.countDown();
                }
            });

            // verify dispatched event
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();
            Assert.assertNotNull(event);
            Assert.assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            Assert.assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());

            // verify callback response
            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put(MessagingTestConstants.EventDataKeys.Messaging.RESPONSE_ERROR, 1);
            final Event responseEvent = new Event.Builder(
                    MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                    MessagingTestConstants.EventType.MESSAGING,
                    MessagingTestConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            try {
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail("getPropositionsForSurfaces callback not called");
            }
            Assert.assertNotNull(responseError[0]);
            Assert.assertEquals(AdobeError.CALLBACK_TIMEOUT, responseError[0]);
            Assert.assertNull(responseMapForSurface[0]);

        });
    }

    @Test
    public void testGetPropositionsForSurfacePaths_propositionsMissingInResponseEventData() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            Surface feedSurface = new Surface("apifeed");
            surfacePaths.add(feedSurface);

            // test
            CountDownLatch latch = new CountDownLatch(1);
            final AdobeError[] responseError = new AdobeError[1];
            final Map<Surface, List<Proposition>>[] responseMapForSurface = new Map[]{null};
            Messaging.getPropositionsForSurfaces(surfacePaths, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError[0] = adobeError;
                    latch.countDown();
                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                    responseMapForSurface[0] = surfaceListMap;
                    latch.countDown();
                }
            });

            // verify dispatched event
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();
            Assert.assertNotNull(event);
            Assert.assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            Assert.assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());

            // verify callback response
            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("SomeKey", "SomeValue");
            final Event responseEvent = new Event.Builder(
                    MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                    MessagingTestConstants.EventType.MESSAGING,
                    MessagingTestConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            try {
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail("getPropositionsForSurfaces callback not called");
            }
            Assert.assertNotNull(responseError[0]);
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError[0]);
            Assert.assertNull(responseMapForSurface[0]);

        });
    }

    @Test
    public void testGetPropositionsForSurfacePaths_invalidPropositionsInResponseEventData() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        runWithMockedMobileCore(eventCaptor, callbackCaptor, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            Surface feedSurface = new Surface("apifeed");
            surfacePaths.add(feedSurface);

            // test
            CountDownLatch latch = new CountDownLatch(1);
            final AdobeError[] responseError = new AdobeError[1];
            final Map<Surface, List<Proposition>>[] responseMapForSurface = new Map[]{null};
            Messaging.getPropositionsForSurfaces(surfacePaths, new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError[0] = adobeError;
                    latch.countDown();
                }

                @Override
                public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                    responseMapForSurface[0] = surfaceListMap;
                    latch.countDown();
                }
            });

            // verify dispatched event
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();
            Assert.assertNotNull(event);
            Assert.assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            Assert.assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());

            // verify callback response
            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put(MessagingTestConstants.EventDataKeys.Messaging.PROPOSITIONS,
                    new ArrayList<Map<String, Object>>() {{
                        new HashMap<String, Object>() {{
                            put("SomeKey", "SomeValue");
                        }};
                    }});
            final Event responseEvent = new Event.Builder(
                    MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                    MessagingTestConstants.EventType.MESSAGING,
                    MessagingTestConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            try {
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail("getPropositionsForSurfaces callback not called");
            }
            Assert.assertNotNull(responseError[0]);
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError[0]);
            Assert.assertNull(responseMapForSurface[0]);

        });
    }

    // ========================================================================================
    // updatePropositionsForSurfaces
    // ========================================================================================
    @Test
    public void test_updatePropositionsForSurfaces() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            surfacePaths.add(new Surface("promos/feed1"));
            surfacePaths.add(new Surface("promos/feed2"));
            // test
            Messaging.updatePropositionsForSurfaces(surfacePaths);

            // verify
            MobileCore.dispatchEvent(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getAllValues().get(0);
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(true, DataReader.optBoolean(eventData, MessagingTestConstants.EventDataKeys.Messaging.UPDATE_PROPOSITIONS, false));
            List<Map<String, Object>> capturedSurfaces = DataReader.optTypedListOfMap(Object.class, eventData, MessagingTestConstants.EventDataKeys.Messaging.SURFACES, null);
            assertEquals(2, capturedSurfaces.size());
            // need to copy the list as capturedSurfaces is unmodifiable
            List<String> sortedList = new ArrayList<>();
            for (Map<String, Object> flattenedSurface : capturedSurfaces) {
                try {
                    sortedList.add(DataReader.getString(flattenedSurface, "uri"));
                } catch (DataReaderException e) {
                    fail(e.getMessage());
                }
            }
            sortedList.sort(null);
            assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1", sortedList.get(0));
            assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2", sortedList.get(1));
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(MessagingTestConstants.EventName.UPDATE_PROPOSITIONS, event.getName());
        });
    }

    @Test
    public void test_updatePropositionsForSurfaces_whenSomeSurfacePathsInvalid_thenOnlyValidPathsUsedForFeedRetrieval() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {
            List<Surface> surfacePaths = new ArrayList<>();
            surfacePaths.add(new Surface("promos/feed1"));
            surfacePaths.add(new Surface("##invalid"));
            surfacePaths.add(new Surface("##alsoinvalid"));
            surfacePaths.add(new Surface("promos/feed3"));

            // test
            Messaging.updatePropositionsForSurfaces(surfacePaths);

            // verify
            MobileCore.dispatchEvent(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getAllValues().get(0);
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(true, DataReader.optBoolean(eventData, MessagingTestConstants.EventDataKeys.Messaging.UPDATE_PROPOSITIONS, false));
            List<Map<String, Object>> capturedSurfaces = DataReader.optTypedListOfMap(Object.class, eventData, MessagingTestConstants.EventDataKeys.Messaging.SURFACES, null);
            assertEquals(2, capturedSurfaces.size());
            // need to copy the list as capturedSurfaces is unmodifiable
            List<String> sortedList = new ArrayList<>();
            for (Map<String, Object> flattenedSurface : capturedSurfaces) {
                try {
                    sortedList.add(DataReader.getString(flattenedSurface, "uri"));
                } catch (DataReaderException e) {
                    fail(e.getMessage());
                }
            }
            sortedList.sort(null);
            assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1", sortedList.get(0));
            assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed3", sortedList.get(1));
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(MessagingTestConstants.EventName.UPDATE_PROPOSITIONS, event.getName());
        });
    }

    @Test
    public void test_updatePropositionsForSurfaces_whenEmptyListProvided_thenNoUpdateMessageFeedsEventDispatched() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        runWithMockedMobileCore(eventCaptor, null, null, () -> {

            // test
            Messaging.updatePropositionsForSurfaces(new ArrayList<>());

            // verify
            MobileCore.dispatchEvent(eventCaptor.capture());

            // verify no event dispatched
            assertNull(eventCaptor.getValue());
        });
    }
}