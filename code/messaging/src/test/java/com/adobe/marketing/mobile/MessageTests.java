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

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, App.class, MessagingState.class})
public class MessageTests {

    private Message message;
    // for use
    private MessagingFullscreenMessage messagingFullscreenMessage;
    private Map<String, Object> consequence = new HashMap<>();
    Map<String, Object> details = new HashMap<>();
    private EventHub eventHub;
    private static String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";

    @Mock
    Core mockCore;
    @Mock
    Activity mockActivity;
    @Mock
    MessagesMonitor mockMessagesMonitor;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    UIService mockUIService;
    @Mock
    MessagingFullscreenMessage mockMessagingFullscreenMessage;
    @Mock
    Message mockMessage;
    @Mock
    MessagingState mockMessagingState;
    @Mock
    MessagingInternal mockMessagingInternal;
    @Captor
    ArgumentCaptor<Event> eventArgumentCaptor;

    class CustomDelegate extends MessageDelegate {
        private boolean showMessage = true;

        @Override
        public boolean shouldShowMessage(UIService.FullscreenMessage fullscreenMessage) {
            if(!showMessage) {
                Message message = (Message)fullscreenMessage.getParent();
                message.track("suppressed");
            }
            return showMessage;
        }

        public void setShowMessage(boolean showMessage) {
            this.showMessage = showMessage;
        }
    }

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        Mockito.when(App.getCurrentActivity()).thenReturn(mockActivity);
        Mockito.when(MobileCore.getCore()).thenReturn(mockCore);
        Mockito.when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Object.class))).thenReturn(mockMessagingFullscreenMessage);
        Mockito.when(mockMessagingFullscreenMessage.getParent()).thenReturn(mockMessage);
        Mockito.when(mockMessagingState.getExperienceEventDatasetId()).thenReturn("datasetId");

        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        try {
            message = new Message(mockMessagingInternal, consequence);
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_messageShow() {
        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        // test
        message.show();

        // verify MessagingFullscreenMessage show called
        verify(mockMessagingFullscreenMessage, times(1)).show();

        // verify triggered tracking event
        verify(mockMessagingInternal).handleTrackingInfo(eventArgumentCaptor.capture());
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedData);
    }

    @Test
    public void test_messageShow_withShowMessageTrueInCustomDelegate() {
        // setup custom delegate, show message is true by default
        CustomDelegate customDelegate = new CustomDelegate();
        // setup mocks
        messagingFullscreenMessage = new MessagingFullscreenMessage("html", customDelegate, false, mockMessagesMonitor, message);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Object.class))).thenReturn(mockMessagingFullscreenMessage);
        try {
            message = new Message(mockMessagingInternal, consequence);
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        // set custom delegate in Message object
        Whitebox.setInternalState(message, "customDelegate", customDelegate);

        // test
        message.show();

        // verify MessagingFullscreenMessage show called
        verify(mockMessagingFullscreenMessage, times(1)).show();

        // verify triggered tracking event
        verify(mockMessagingInternal).handleTrackingInfo(eventArgumentCaptor.capture());
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedData);
    }

    @Test
    public void test_messageShow_withShowMessageFalseInCustomDelegate() {
        // setup custom delegate
        CustomDelegate customDelegate = new CustomDelegate();
        customDelegate.setShowMessage(false);
        // setup mocks
        messagingFullscreenMessage = new MessagingFullscreenMessage("html", customDelegate, false, mockMessagesMonitor, message);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Object.class))).thenReturn(messagingFullscreenMessage);
        try {
            message = new Message(mockMessagingInternal, consequence);
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // setup expected events
        HashMap<String, Object> expectedTriggeredEventData = new HashMap<>();
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        HashMap<String, Object> expectedSuppressedEventData = new HashMap<>();
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "suppressed");

        // test
        message.show();

        // expect 2 events: triggered tracking + suppressed tracking
        verify(mockMessagingInternal, times(2)).handleTrackingInfo(eventArgumentCaptor.capture());
        List<Event> capturedEvents = eventArgumentCaptor.getAllValues();
        // verify triggered tracking event
        Event triggeredTrackingEvent = capturedEvents.get(0);
        assertEquals(triggeredTrackingEvent.getEventData(), expectedTriggeredEventData);

        // verify custom delegate tracking event
        Event suppressedTrackingEvent = capturedEvents.get(1);
        assertEquals(suppressedTrackingEvent.getEventData(), expectedSuppressedEventData);
    }

    @Test
    public void test_messageDismiss() {
        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "dismissed");

        // test
        message.dismiss();

        // verify MessagingFullscreenMessage dismiss called
        verify(mockMessagingFullscreenMessage, times(1)).dismiss();

        // verify dismissed tracking event
        verify(mockMessagingInternal).handleTrackingInfo(eventArgumentCaptor.capture());
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedData);
    }

    @Test
    public void test_messageShowFailure() {
        // setup mocks
        when(mockMessagesMonitor.isDisplayed()).thenReturn(true);
        messagingFullscreenMessage = new MessagingFullscreenMessage("html", mockMessage,false, mockMessagesMonitor, message);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Object.class))).thenReturn(messagingFullscreenMessage);
        try {
            message = new Message(mockMessagingInternal, consequence);
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // test
        message.show();

        // verify onShowFailure called
        verify(mockMessage, times(1)).onShowFailure();
    }

    @Test
    public void test_overrideUrlLoad() {
        // setup
        message.messagingInternal = mockMessagingInternal;
        when(mockMessagingFullscreenMessage.getParent()).thenReturn(message);

        // setup expected events
        HashMap<String, Object> expectedDeepLinkClickedTrackingData = new HashMap<>();
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "deeplinkclicked");

        HashMap<String, Object> expectedDismissedTrackingData = new HashMap<>();
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "123456789");
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "dismissed");

        // test
        message.overrideUrlLoad(mockMessagingFullscreenMessage, "adbinapp://dismiss?interaction=deeplinkclicked&link=https://adobe.com");

        // expect 2 events: deeplink click tracking + dismissed tracking
        verify(mockMessagingInternal, times(2)).handleTrackingInfo(eventArgumentCaptor.capture());
        List<Event> capturedEvents = eventArgumentCaptor.getAllValues();
        // verify triggered tracking event
        Event triggeredTrackingEvent = capturedEvents.get(0);
        assertEquals(triggeredTrackingEvent.getEventData(), expectedDeepLinkClickedTrackingData);

        // verify custom delegate tracking event
        Event suppressedTrackingEvent = capturedEvents.get(1);
        assertEquals(suppressedTrackingEvent.getEventData(), expectedDismissedTrackingData);

        // verify showUrl called
        verify(mockUIService, times(1)).showUrl("https://adobe.com");
    }
}
