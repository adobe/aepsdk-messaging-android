/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventHandler;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageSettings;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import com.adobe.marketing.mobile.util.DefaultPresentationUtilityProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PresentableMessageMapperTests {
    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    UIService mockUIService;
    @Mock
    UriOpening mockUriOpening;
    @Mock
    Message mockMessage;
    @Mock
    Presentable<InAppMessage> mockInAppPresentable;
    @Mock
    InAppMessage mockPresentation;
    @Mock
    PresentationError mockPresentationError;
    @Mock
    InAppMessageEventHandler mockEventHandler;
    @Mock
    InAppMessageSettings mockMessageSettings;
    @Mock
    MessagingExtension mockMessagingExtension;

    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private PresentableMessageMapper.InternalMessage internalMessage;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        PresentableMessageMapper.getInstance().clearPresentableMessageMap();
        reset(mockExtensionApi);
        reset(mockServiceProvider);
        reset(mockUIService);
        reset(mockUriOpening);
        reset(mockMessage);
        reset(mockInAppPresentable);
        reset(mockPresentation);
        reset(mockMessageSettings);
        reset(mockPresentationError);
        reset(mockEventHandler);
        reset(mockMessagingExtension);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getUriService()).thenReturn(mockUriOpening);
            when(mockUIService.create(any(InAppMessage.class), any())).thenReturn(mockInAppPresentable);
            when(mockInAppPresentable.getPresentation()).thenReturn(mockPresentation);
            when(mockPresentation.getId()).thenReturn("mockId");

            runnable.run();
        }
    }

    RuleConsequence createRuleConsequence() {
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, html);
        data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, data);
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, MessagingConstants.SchemaValues.SCHEMA_IAM);
        return new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);
    }

    // ========================================================================================
    // createMessage tests
    // ========================================================================================
    @Test
    public void test_createMessage() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // verify
            verify(mockUIService, times(1)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_MessagesForSameConsequence() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            PresentableMessageMapper.InternalMessage internalMessageSameConsequence = null;
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
                internalMessageSameConsequence = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // verify
            verify(mockUIService, times(1)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
            assertEquals(internalMessage, internalMessageSameConsequence);
        });
    }

    @Test
    public void test_createMessage_MissingConsequenceDetails() {
        // setup
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, null);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_MissingConsequenceId() {
        // setup
        RuleConsequence mockConsequence = mock(RuleConsequence.class);
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, html);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, data);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, MessagingConstants.SchemaValues.SCHEMA_IAM);
        when(mockConsequence.getId()).thenReturn(null);
        when(mockConsequence.getDetail()).thenReturn(details);
        when(mockConsequence.getType()).thenReturn(MessagingConstants.SchemaValues.SCHEMA_IAM);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, mockConsequence, new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_InvalidSchema() {
        // setup
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, html);
        data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, data);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "notASchema");
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_DetailsMissingHtml() {
        // setup
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        data.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, null);
        data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, data);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, MessagingConstants.SchemaValues.SCHEMA_IAM);
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_DetailsMissingData() {
        // setup
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, null);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, MessagingConstants.SchemaValues.SCHEMA_IAM);
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_NullUIService() {
        // setup
        runUsingMockedServiceProvider(() -> {
            when(mockServiceProvider.getUIService()).thenReturn(null);
            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                assertEquals(IllegalStateException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }

    @Test
    public void test_createMessage_WithMessageSettings() {
        // setup
        runUsingMockedServiceProvider(() -> {
            Map<String, String> gestureMap = new HashMap<>();
            Map<String, Object> rawMessageSettings = new HashMap();
            gestureMap.put("backgroundTap", "adbinapp://dismiss");
            gestureMap.put("swipeLeft", "adbinapp://dismiss?interaction=negative");
            gestureMap.put("swipeRight", "adbinapp://dismiss?interaction=positive");
            gestureMap.put("swipeUp", "adbinapp://dismiss");
            gestureMap.put("swipeDown", "adbinapp://dismiss");
            rawMessageSettings.put("width", 100);
            rawMessageSettings.put("height", 100);
            rawMessageSettings.put("backdropColor", "808080");
            rawMessageSettings.put("backdropOpacity", 0.5f);
            rawMessageSettings.put("cornerRadius", 70.0f);
            rawMessageSettings.put("dismissAnimation", "fade");
            rawMessageSettings.put("displayAnimation", "bottom");
            rawMessageSettings.put("gestures", gestureMap);
            rawMessageSettings.put("horizontalAlign", "right");
            rawMessageSettings.put("horizontalInset", 5);
            rawMessageSettings.put("verticalAlign", "top");
            rawMessageSettings.put("verticalInset", 10);
            rawMessageSettings.put("uiTakeover", true);

            // test
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), rawMessageSettings, new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // verify
            verify(mockUIService, times(1)).create(any(InAppMessage.class), any(DefaultPresentationUtilityProvider.class));
        });
    }
    
    // ========================================================================================
    // Message getId
    // ========================================================================================
    @Test
    public void test_messageGetId() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            String id = internalMessage.getId();

            // verify
            assertEquals("123456789", id);
        });
    }

    // ========================================================================================
    // Message set/getAutoTrack
    // ========================================================================================
    @Test
    public void test_messageSetGetAutoTrack() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            internalMessage.setAutoTrack(false);

            // verify
            assertFalse(internalMessage.getAutoTrack());
        });
    }

    // ========================================================================================
    // Message show, dismiss, and trigger tests
    // ========================================================================================
    @Test
    public void test_messageShow() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            internalMessage.show();

            // verify fullscreen message show called
            verify(mockInAppPresentable, times(1)).show();
        });
    }

    @Test
    public void test_messageDismiss_suppressAutoTrackFalse() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            internalMessage.dismiss(false);

            // verify fullscreen message dismiss called
            verify(mockInAppPresentable, times(1)).dismiss();

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(PresentableMessageMapper.InternalMessage.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISMISS);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageDismiss_suppressAutoTrackTrue() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            internalMessage.dismiss(true);

            // verify fullscreen message dismiss called
            verify(mockInAppPresentable, times(1)).dismiss();

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any(String.class), any(MessagingEdgeEventType.class), any(PresentableMessageMapper.InternalMessage.class));
        });
    }

    @Test
    public void test_messageTrigger() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            internalMessage.trigger();

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(PresentableMessageMapper.InternalMessage.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_TRIGGER);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageTrigger_autoTrackFalse() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            internalMessage.setAutoTrack(false);
            internalMessage.trigger();

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any(String.class), any(MessagingEdgeEventType.class), any(PresentableMessageMapper.InternalMessage.class));
        });
    }

    // ========================================================================================
    // Message track tests
    // ========================================================================================
    @Test
    public void test_messageTrack() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            internalMessage.track("mock track", MessagingEdgeEventType.IN_APP_INTERACT);

            // verify tracking event
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(PresentableMessageMapper.InternalMessage.class));
            MessagingEdgeEventType displayTrackingEvent = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, displayTrackingEvent);
            assertEquals("mock track", interaction);
        });
    }

    @Test
    public void test_messageTrackWithMissingMessagingEdgeEventType() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            internalMessage.track("mock track", null);

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any(String.class), any(MessagingEdgeEventType.class), any(PresentableMessageMapper.InternalMessage.class));
        });
    }

    // ========================================================================================
    // getMessageFromPresentableId
    // ========================================================================================
    @Test
    public void test_getMessageFromPresentableId() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                internalMessage = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().createMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), null);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            Message message = PresentableMessageMapper.getInstance().getMessageFromPresentableId("mockId");

            // verify
            assertEquals(internalMessage, message);
        });
    }

    @Test
    public void test_getMessageFromPresentableId_MessageNotAvailable() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            Message message = PresentableMessageMapper.getInstance().getMessageFromPresentableId("newMockId");

            // verify
            assertNull(message);
        });
    }

    @Test
    public void test_getMessageFromPresentableId_PresentableIdIsNull() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            Message message = PresentableMessageMapper.getInstance().getMessageFromPresentableId(null);

            // verify
            assertNull(message);
        });
    }

    @Test
    public void test_getMessageFromPresentableId_PresentableIdIsEmpty() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            Message message = PresentableMessageMapper.getInstance().getMessageFromPresentableId("");

            // verify
            assertNull(message);
        });
    }
}
