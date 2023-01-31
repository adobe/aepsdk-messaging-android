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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.messaging.internal.MessageRequiredFieldMissingException;
import com.adobe.marketing.mobile.messaging.internal.MessageTestConfig;
import com.adobe.marketing.mobile.messaging.internal.MessagingDelegate;
import com.adobe.marketing.mobile.messaging.internal.MessagingEdgeEventType;
import com.adobe.marketing.mobile.messaging.internal.MessagingExtension;
import com.adobe.marketing.mobile.messaging.internal.MessagingTestConstants;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.internal.MessagesMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessageTests {
    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Application mockApplication;
    @Mock
    Context mockContext;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    DeviceInforming mockDeviceInfoService;
    @Mock
    UIService mockUIService;
    @Mock
    FullscreenMessage mockFullscreenMessage;
    @Mock
    FullscreenMessageDelegate mockFullscreenMessageDelegate;
    @Mock
    MessagingExtension mockMessagingExtension;
    @Mock
    AppContextService mockAppContextService;
    @Mock
    Looper mockLooper;
    @Mock
    WebView mockWebView;
    @Mock
    Handler mockHandler;
    @Mock
    MessagesMonitor mockMessagesMonitor;

    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private Message message;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
        reset(mockApplication);
        reset(mockContext);
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
        reset(mockUIService);
        reset(mockFullscreenMessage);
        reset(mockFullscreenMessageDelegate);
        reset(mockMessagingExtension);
        reset(mockAppContextService);
        reset(mockLooper);
        reset(mockWebView);
        reset(mockHandler);
        reset(mockMessagesMonitor);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockServiceProvider.getMessageDelegate()).thenReturn(mockFullscreenMessageDelegate);
            when(mockFullscreenMessageDelegate.shouldShowMessage(any(FullscreenMessage.class))).thenReturn(true);
            when(mockUIService.createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class))).thenReturn(mockFullscreenMessage);
            when(mockAppContextService.getApplication()).thenReturn(mockApplication);
            when(mockApplication.getMainLooper()).thenReturn(mockLooper);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            // Actually run the handler runnable - mocking the handler.post()
            doAnswer(invocation -> {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }).when(mockHandler).post(any(Runnable.class));

            runnable.run();
        }
    }

    Map<String, Object> setupXdmMap(MessageTestConfig config) {
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experiance = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        if (!config.isMissingMessageId) {
            messageExecution.put("messageExecutionID", "123456789");
        }
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
        return xdm;
    }

    RuleConsequence createRuleConsequence(Map<String, Object> xdmMap) {
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        return new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);
    }

    // ========================================================================================
    // Message constructor tests
    // ========================================================================================
    @Test
    public void test_messageConstructor() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // test
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // verify
            verify(mockUIService, times(1)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
        });
    }

    @Test
    public void test_messageConstructor_MissingConsequenceDetails() {
        // setup
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, null);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                message = new Message(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
        });
    }

    @Test
    public void test_messageConstructor_NotCJMConsequenceType() {
        // setup
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        RuleConsequence consequence = new RuleConsequence("123456789", "otherRuleType", details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                message = new Message(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
        });
    }

    @Test
    public void test_messageConstructor_MissingConsequenceId() {
        // setup
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        RuleConsequence consequence = new RuleConsequence("", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                message = new Message(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
        });
    }

    @Test
    public void test_messageConstructor_DetailsMissingHtml() {
        // setup
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        RuleConsequence consequence = new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

        runUsingMockedServiceProvider(() -> {
            // test
            try {
                message = new Message(mockMessagingExtension, consequence, new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                assertEquals(MessageRequiredFieldMissingException.class, exception.getClass());
            }

            // verify
            verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
        });
    }

    // ========================================================================================
    // Message show, dismiss, and trigger tests
    // ========================================================================================
    @Test
    public void test_messageShow() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            message.show();

            // verify fullscreen message show called
            verify(mockFullscreenMessage, times(1)).show(eq(false));

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageShow_withShowMessageTrueInCustomDelegate() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            // setup custom delegate, show message is true by default
            CustomMessagingDelegate customMessageDelegate = new CustomMessagingDelegate();
            when(mockServiceProvider.getMessageDelegate()).thenReturn(customMessageDelegate);
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.show();

            // verify fullscreen message show called
            verify(mockFullscreenMessage, times(1)).show(eq(false));

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageShow_withShowMessageFalseInCustomDelegate() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            // setup custom delegate, show message is true by default
            CustomMessagingDelegate customMessageDelegate = new CustomMessagingDelegate();
            customMessageDelegate.setShowMessage(false);
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            when(mockServiceProvider.getMessageDelegate()).thenReturn(customMessageDelegate);
            when(mockFullscreenMessage.getParent()).thenReturn(message);

            // test
            message.show(false);

            // verify fullscreen message show called
            verify(mockFullscreenMessage, times(1)).show(eq(false));

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageDismiss_suppressAutoTrackFalse() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            message.dismiss(false);

            // verify fullscreen message dismiss called
            verify(mockFullscreenMessage, times(1)).dismiss();

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISMISS);
            assertEquals(null, interaction);
        });
    }

    @Test
    public void test_messageDismiss_suppressAutoTrackTrue() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            message.dismiss(true);

            // verify fullscreen message dismiss called
            verify(mockFullscreenMessage, times(1)).dismiss();

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), any(Message.class));
        });
    }

    @Test
    public void test_messageTrigger() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            // test
            message.trigger();

            // verify tracking event data
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
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
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.autoTrack = false;
            message.trigger();

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), any(Message.class));
        });
    }

    // ========================================================================================
    // Message overrideUrlLoad tests
    // ========================================================================================
    @Test
    public void test_overrideUrlLoad() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            MessageSettings settings = new MessageSettings();
            settings.setParent(message);
            when(mockFullscreenMessage.getMessageSettings()).thenReturn(settings);

            // test
            message.overrideUrlLoad(mockFullscreenMessage, "adbinapp://dismiss?interaction=deeplinkclicked&link=https://adobe.com");

            // expect 1 event: deeplink click tracking
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
            List<MessagingEdgeEventType> capturedEvents = messagingEdgeEventTypeArgumentCaptor.getAllValues();
            List<String> capturedInteractions = interactionArgumentCaptor.getAllValues();
            // verify interact tracking event
            MessagingEdgeEventType displayTrackingEvent = capturedEvents.get(0);
            String interaction = capturedInteractions.get(0);
            assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, displayTrackingEvent);
            assertEquals("deeplinkclicked", interaction);

            // verify showUrl called
            verify(mockUIService, times(1)).showUrl("https://adobe.com");
        });
    }

    @Test
    public void test_overrideUrlLoadWhenUrlEmpty() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            MessageSettings settings = new MessageSettings();
            settings.setParent(message);
            when(mockFullscreenMessage.getMessageSettings()).thenReturn(settings);

            // test
            message.overrideUrlLoad(mockFullscreenMessage, "");

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), any(Message.class));

            // verify showUrl not called
            verify(mockUIService, times(0)).showUrl(anyString());
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
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.track("mock track", MessagingEdgeEventType.IN_APP_INTERACT);

            // verify tracking event
            verify(mockMessagingExtension, times(1)).sendPropositionInteraction(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture(), any(Message.class));
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
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>());
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.track("mock track", null);

            // verify no tracking event
            verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), any(Message.class));
        });
    }

    // ========================================================================================
    // Message javascript handling and loading tests
    // ========================================================================================
    @Test
    public void test_handleJavascript() {
        // setup
        AdobeCallback<String> callback = s -> assertEquals("hello world", s);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), mockWebView, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage("test", callback);
            message.evaluateJavascript("(function test(hello world) { return(arg); })()");

            // verify evaluate javascript called
            verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
        });
    }

    @Test
    public void test_handleJavascript_when_webviewNull() {
        // setup
        AdobeCallback<String> callback = s -> assertEquals("hello world", s);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), null, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage("test", callback);
            message.evaluateJavascript("(function test(hello world) { return(arg); })()");

            // verify evaluate javascript not called
            verify(mockWebView, times(0)).evaluateJavascript(anyString(), any(ValueCallback.class));
        });
    }


    @Test
    public void test_handleJavascript_withNoCallback() {
        // setup
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), mockWebView, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage("test", null);
            message.evaluateJavascript("(function test(hello world) { return(arg); })()");

            // verify evaluate javascript called
            verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
        });
    }

    @Test
    public void test_handleJavascript_withNoMessageName() {
        // setup
        AdobeCallback<String> callback = s -> assertEquals("hello world", s);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), mockWebView, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage(null, callback);
            message.evaluateJavascript("(function test(hello world) { return(arg); })()");

            // verify evaluate javascript not called
            verify(mockWebView, times(0)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
        });
    }

    @Test
    public void test_handleJavascript_withDuplicateMessageNames_thenOnlyOneWebViewJavascriptInterfaceCreated() {
        // setup
        AdobeCallback<String> callback = s -> assertEquals("hello world", s);
        AdobeCallback<String> callback2 = s -> assertEquals("hello world", s);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), mockWebView, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage("test", callback);
            message.handleJavascriptMessage("test", callback2);
            message.evaluateJavascript("(function test(hello world) { return(arg); })()");

            // verify only one WebViewJavascriptInterface created
            Map<String, WebViewJavascriptInterface> scriptHandlers = message.getScriptHandlers();
            assertEquals(1, scriptHandlers.size());
            // verify evaluate javascript called
            verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
        });
    }


    @Test
    public void test_loadJavascriptWhenJavascriptIsNull() {
        // setup
        AdobeCallback<String> callback = s -> assertEquals("hello world", s);
        runUsingMockedServiceProvider(() -> {
            try {
                message = new Message(mockMessagingExtension, createRuleConsequence(setupXdmMap(new MessageTestConfig())), new HashMap<>(), new HashMap<>(), mockWebView, mockHandler);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }

            // test
            message.handleJavascriptMessage("test", callback);
            message.evaluateJavascript(null);

            // verify evaluate javascript not called
            verify(mockWebView, times(0)).evaluateJavascript(anyString(), any(ValueCallback.class));
        });
    }

    class CustomMessagingDelegate extends MessagingDelegate {
        private boolean showMessage = true;

        @Override
        public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
            if (!showMessage) {
                Message message = (Message) fullscreenMessage.getParent();
                message.track("suppressed", MessagingEdgeEventType.IN_APP_DISPLAY);
            }
            return showMessage;
        }

        public void setShowMessage(boolean showMessage) {
            this.showMessage = showMessage;
        }
    }
}
