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

import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, Intent.class})
public class MessagingTests {
    @Mock
    Intent mockIntent;

    @Before
    public void before() {
        PowerMockito.mockStatic(MobileCore.class);
    }

    // ========================================================================================
    // extensionVersion
    // ========================================================================================

    @Test
    public void test_extensionVersionAPI() {
        // test
        String extensionVersion = Messaging.extensionVersion();
        Assert.assertEquals("The Extension version API returns the correct value", MessagingConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    // ========================================================================================
    // registerExtension
    // ========================================================================================

    @Test
    public void test_registerExtensionAPI() {
        // test
        Messaging.registerExtension();
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // The monitor extension should register with core
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(ArgumentMatchers.eq(MessagingInternal.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        Assert.assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // should not crash on calling the callback
        extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
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
        mockDataMap.put(MessagingConstants.TrackingKeys._XDM, mockXDMData);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(1)).putExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockXDMData);
    }

    // ========================================================================================
    // handleNotificationResponse
    // ========================================================================================
    @Test
    public void test_handleNotificationResponse_WhenParamsAreNull() {
        // test
        Messaging.handleNotificationResponse(null, false, null);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleNotificationResponse() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        String mockActionId = "mockActionId";
        String mockXdm = "mockXdm";

        try {
            PowerMockito.whenNew(Intent.class)
                    .withNoArguments().thenReturn(mockIntent);
        } catch (Exception e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "Intent exception");
        }

        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

        // verify
        verify(mockIntent, times(2)).getStringExtra(anyString());

        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        EventData eventData = event.getData();
        assertNotNull(eventData);
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), event.getEventType().getName());
        try {
            assertEquals(eventData.getString2(TRACK_INFO_KEY_ACTION_ID), mockActionId);
        } catch (VariantException e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "getString2 variant exception, error : %s", e.getMessage());
        }
    }
}
