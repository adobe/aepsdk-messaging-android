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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.internal.MessagesMonitor;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, App.class, MessagingState.class, ServiceProvider.class})
public class MessageTests {

    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private final Map<String, Object> consequence = new HashMap<>();
    Map<String, Object> details = new HashMap<>();
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
    AEPMessage mockAEPMessage;
    @Mock
    Message mockMessage;
    @Mock
    AEPMessageSettings mockAEPMessageSettings;
    @Mock
    MessagingState mockMessagingState;
    @Mock
    MessagingInternal mockMessagingInternal;
    @Mock
    ServiceProvider mockServiceProvider;
    @Captor
    ArgumentCaptor<Event> eventArgumentCaptor;
    private Message message;
    private AEPMessage aepMessage;
    private EventHub eventHub;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);

        Mockito.when(App.getCurrentActivity()).thenReturn(mockActivity);
        Mockito.when(MobileCore.getCore()).thenReturn(mockCore);
        when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
        when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(FullscreenMessageDelegate.class), any(boolean.class), any(MessageSettings.class))).thenReturn(mockAEPMessage);
        Mockito.when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        Mockito.when(mockAEPMessageSettings.getParent()).thenReturn(mockMessage);
        Mockito.when(mockMessagingState.getExperienceEventDatasetId()).thenReturn("datasetId");

        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experiance = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experiance.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experiance);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_messageShow() {
        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        // test
        message.show();

        // verify aepMessage show called
        verify(mockAEPMessage, times(1)).show();

        // verify tracking event data
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, trackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), trackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), trackingEvent.getSource());
    }

    @Test
    public void test_messageShow_withShowMessageTrueInCustomDelegate() {
        Mockito.when(mockAEPMessageSettings.getParent()).thenReturn(message);
        // setup custom delegate, show message is true by default
        CustomDelegate customDelegate = new CustomDelegate();
        // setup mocks
        try {
            aepMessage = new AEPMessage("html", customDelegate, false, mockMessagesMonitor, mockAEPMessageSettings);
        } catch (MessageCreationException e) {
            fail(e.getLocalizedMessage());
        }
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(FullscreenMessageDelegate.class), any(boolean.class), any(MessageSettings.class))).thenReturn(mockAEPMessage);
        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        // set custom delegate in Message object
        Whitebox.setInternalState(message, "customDelegate", customDelegate);

        // test
        message.show();

        // verify aepMessage show called
        verify(mockAEPMessage, times(1)).show();

        // verify tracking event data
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(expectedData, trackingEvent.getEventData());
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, trackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), trackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), trackingEvent.getSource());
    }

    @Test
    public void test_messageShow_withShowMessageFalseInCustomDelegate() {
        Mockito.when(mockAEPMessageSettings.getParent()).thenReturn(message);
        // setup custom delegate
        CustomDelegate customDelegate = new CustomDelegate();
        customDelegate.setShowMessage(false);
        // setup mocks
        try {
            aepMessage = new AEPMessage("html", customDelegate, false, mockMessagesMonitor, mockAEPMessageSettings);
        } catch (MessageCreationException e) {
            fail(e.getLocalizedMessage());
        }
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(FullscreenMessageDelegate.class), any(boolean.class), any(MessageSettings.class))).thenReturn(aepMessage);
        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // setup expected events
        HashMap<String, Object> expectedTriggeredEventData = new HashMap<>();
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedTriggeredEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "triggered");

        HashMap<String, Object> expectedSuppressedEventData = new HashMap<>();
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedSuppressedEventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "suppressed");

        // test
        message.show();

        // expect 2 tracking events: triggered tracking + suppressed tracking
        verify(mockMessagingInternal, times(2)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        List<Event> capturedEvents = eventArgumentCaptor.getAllValues();
        // verify triggered tracking event
        Event triggeredTrackingEvent = capturedEvents.get(0);
        assertEquals(triggeredTrackingEvent.getEventData(), expectedTriggeredEventData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, triggeredTrackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), triggeredTrackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), triggeredTrackingEvent.getSource());

        // verify custom delegate tracking event
        Event suppressedTrackingEvent = capturedEvents.get(1);
        assertEquals(suppressedTrackingEvent.getEventData(), expectedSuppressedEventData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, suppressedTrackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), suppressedTrackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), suppressedTrackingEvent.getSource());
    }

    @Test
    public void test_messageDismiss() {
        // setup expected event
        HashMap<String, Object> expectedData = new HashMap<>();
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "dismissed");

        // test
        message.dismiss();

        // verify aepMessage dismiss called
        verify(mockAEPMessage, times(1)).dismiss();

        // verify dismissed tracking event
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, trackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), trackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), trackingEvent.getSource());
    }

    @Test
    public void test_messageShowFailure() {
        // setup mocks
        when(mockMessagesMonitor.isDisplayed()).thenReturn(true);
        try {
            aepMessage = new AEPMessage("html", mockMessage, false, mockMessagesMonitor, mockAEPMessageSettings);
        } catch (MessageCreationException e) {
            fail(e.getLocalizedMessage());
        }
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(FullscreenMessageDelegate.class), any(boolean.class), any(MessageSettings.class))).thenReturn(aepMessage);
        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
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
        when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        when(mockAEPMessageSettings.getParent()).thenReturn(message);

        // setup expected events
        HashMap<String, Object> expectedDeepLinkClickedTrackingData = new HashMap<>();
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedDeepLinkClickedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "deeplinkclicked");

        HashMap<String, Object> expectedDismissedTrackingData = new HashMap<>();
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedDismissedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "dismissed");

        // test
        message.overrideUrlLoad(mockAEPMessage, "adbinapp://dismiss?interaction=deeplinkclicked&link=https://adobe.com");

        // expect 2 events: deeplink click tracking + dismissed tracking
        verify(mockMessagingInternal, times(2)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        List<Event> capturedEvents = eventArgumentCaptor.getAllValues();
        // verify triggered tracking event
        Event triggeredTrackingEvent = capturedEvents.get(0);
        assertEquals(triggeredTrackingEvent.getEventData(), expectedDeepLinkClickedTrackingData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, triggeredTrackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), triggeredTrackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), triggeredTrackingEvent.getSource());

        // verify custom delegate tracking event
        Event suppressedTrackingEvent = capturedEvents.get(1);
        assertEquals(suppressedTrackingEvent.getEventData(), expectedDismissedTrackingData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, suppressedTrackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), suppressedTrackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), suppressedTrackingEvent.getSource());

        // verify showUrl called
        verify(mockUIService, times(1)).showUrl("https://adobe.com");
    }

    @Test
    public void test_overrideUrlLoadWhenUrlEmpty() {
        // setup
        message.messagingInternal = mockMessagingInternal;
        when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        when(mockAEPMessageSettings.getParent()).thenReturn(message);

        // test
        message.overrideUrlLoad(mockAEPMessage, "");

        // expect 0 events: deeplink click tracking + dismissed tracking
        verify(mockMessagingInternal, times(0)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));

        // verify showUrl not called
        verify(mockUIService, times(0)).showUrl(anyString());
    }

    @Test
    public void test_messageTrack() {
        // setup expected events
        HashMap<String, Object> expectedTrackingData = new HashMap<>();
        expectedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        expectedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, "123456789");
        expectedTrackingData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock track");

        // test
        message.track("mock track");

        // verify mock tracking event
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(eventArgumentCaptor.capture(), any(Map.class));
        Event trackingEvent = eventArgumentCaptor.getValue();
        assertEquals(trackingEvent.getEventData(), expectedTrackingData);
        assertEquals(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_MESSAGING_EVENT, trackingEvent.getName());
        assertEquals(MessagingConstants.EventType.MESSAGING.toLowerCase(), trackingEvent.getType());
        assertEquals(MessagingConstants.EventSource.REQUEST_CONTENT.toLowerCase(), trackingEvent.getSource());
    }

    @Test
    public void test_messageTrackWithInvalidInteractionType() {
        // test
        message.track("");

        // verify mock tracking event
        verify(mockMessagingInternal, times(0)).handleInAppTrackingInfo(any(Event.class), any(Map.class));
    }

    class CustomDelegate extends MessageDelegate {
        private boolean showMessage = true;

        @Override
        public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
            if (!showMessage) {
                AEPMessageSettings settings = (AEPMessageSettings) fullscreenMessage.getSettings();
                Message message = (Message) settings.getParent();
                message.track("suppressed");
            }
            return showMessage;
        }

        public void setShowMessage(boolean showMessage) {
            this.showMessage = showMessage;
        }
    }
}
