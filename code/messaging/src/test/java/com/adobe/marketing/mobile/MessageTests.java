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
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageCreationException;
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
    ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor;
    @Captor
    ArgumentCaptor<String> interactionArgumentCaptor;
    private Message message;
    private AEPMessage aepMessage;
    private EventHub eventHub;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(ServiceProvider.class);

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
        messageExecution.put("messageExecutionID", "123456789");
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
        // test
        message.show();

        // verify aepMessage show called
        verify(mockAEPMessage, times(1)).show();

        // verify tracking event data
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
        String interaction = interactionArgumentCaptor.getValue();
        assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
        assertEquals(null, interaction);
    }

    @Test
    public void test_messageShow_withShowMessageTrueInCustomDelegate() {
        // setup custom delegate, show message is true by default
        CustomDelegate customDelegate = new CustomDelegate();
        ServiceProvider.getInstance().setMessageDelegate(customDelegate);
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

        // set custom delegate in Message object
        Whitebox.setInternalState(message, "fullscreenMessageDelegate", customDelegate);

        // test
        message.show();

        // verify aepMessage show called
        verify(mockAEPMessage, times(1)).show();

        // verify tracking event data
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
        String interaction = interactionArgumentCaptor.getValue();
        assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
        assertEquals(null, interaction);
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

        // test
        message.show();

        // expect 2 tracking events: triggered tracking + suppressed tracking
        verify(mockMessagingInternal, times(2)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        List<MessagingEdgeEventType> capturedEvents = messagingEdgeEventTypeArgumentCaptor.getAllValues();
        List<String> capturedInteractions = interactionArgumentCaptor.getAllValues();
        // verify display tracking event
        MessagingEdgeEventType displayTrackingEvent = capturedEvents.get(0);
        String interaction = capturedInteractions.get(0);
        assertEquals(MessagingEdgeEventType.IN_APP_DISPLAY, displayTrackingEvent);
        assertEquals(null, interaction);

        // verify custom delegate suppressed tracking event
        MessagingEdgeEventType suppressedTrackingEvent = capturedEvents.get(1);
        interaction = capturedInteractions.get(1);
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, suppressedTrackingEvent);
        assertEquals("suppressed", interaction);
    }

    @Test
    public void test_messageDismiss() {
        // test
        message.dismiss();

        // verify aepMessage dismiss called
        verify(mockAEPMessage, times(1)).dismiss();

        // verify dismissed tracking event
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
        String interaction = interactionArgumentCaptor.getValue();
        assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISMISS);
        assertEquals(null, interaction);
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

        // test
        message.overrideUrlLoad(mockAEPMessage, "adbinapp://dismiss?interaction=deeplinkclicked&link=https://adobe.com");

        // expect 2 events: deeplink click tracking + dismissed tracking
        verify(mockMessagingInternal, times(2)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        List<MessagingEdgeEventType> capturedEvents = messagingEdgeEventTypeArgumentCaptor.getAllValues();
        List<String> capturedInteractions = interactionArgumentCaptor.getAllValues();
        // verify interact tracking event
        MessagingEdgeEventType displayTrackingEvent = capturedEvents.get(0);
        String interaction = capturedInteractions.get(0);
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, displayTrackingEvent);
        assertEquals("deeplinkclicked", interaction);

        // verify custom delegate tracking event
        MessagingEdgeEventType dismissTrackingEvent = capturedEvents.get(1);
        interaction = capturedInteractions.get(1);
        assertEquals(MessagingEdgeEventType.IN_APP_DISMISS, dismissTrackingEvent);
        assertEquals(null, interaction);

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
        verify(mockMessagingInternal, times(0)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));

        // verify showUrl not called
        verify(mockUIService, times(0)).showUrl(anyString());
    }

    @Test
    public void test_messageTrack() {
        // test
        message.track("mock track", MessagingEdgeEventType.IN_APP_INTERACT);

        // verify mock tracking event
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        MessagingEdgeEventType displayTrackingEvent = messagingEdgeEventTypeArgumentCaptor.getValue();
        String interaction = interactionArgumentCaptor.getValue();
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, displayTrackingEvent);
        assertEquals("mock track", interaction);
    }

    @Test
    public void test_messageTrackWithMissingMessagingEdgeEventType() {
        // test
        message.track(null, null);

        // verify no tracking event
        verify(mockMessagingInternal, times(0)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));

    }

    class CustomDelegate extends MessageDelegate {
        private boolean showMessage = true;

        @Override
        public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
            if (!showMessage) {
                AEPMessageSettings settings = (AEPMessageSettings) ((AEPMessage) fullscreenMessage).getSettings();
                Message message = (Message) settings.getParent();
                message.track("suppressed", MessagingEdgeEventType.IN_APP_INTERACT);
            }
            return showMessage;
        }

        public void setShowMessage(boolean showMessage) {
            this.showMessage = showMessage;
        }
    }
}
