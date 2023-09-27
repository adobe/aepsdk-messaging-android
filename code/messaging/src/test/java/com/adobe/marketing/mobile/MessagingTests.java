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

import static com.adobe.marketing.mobile.messaging.internal.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
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

import com.adobe.marketing.mobile.messaging.internal.MessagingExtension;
import com.adobe.marketing.mobile.messaging.internal.MessagingTestConstants;
import com.adobe.marketing.mobile.services.DeviceInforming;
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
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");
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

            // verify event
            Event event = eventCaptor.getValue();
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(eventData.get(TRACK_INFO_KEY_ACTION_ID), mockActionId);
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
            assertEquals("mobileapp://mockPackageName/promos/feed1", sortedList.get(0));
            assertEquals("mobileapp://mockPackageName/promos/feed2", sortedList.get(1));
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
            assertEquals("mobileapp://mockPackageName/promos/feed1", sortedList.get(0));
            assertEquals("mobileapp://mockPackageName/promos/feed3", sortedList.get(1));
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
