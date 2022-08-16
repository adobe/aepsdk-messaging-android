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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageCreationException;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageGesture;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAnimation;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAlignment;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.internal.context.App;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, App.class, MessagingState.class, ServiceProvider.class})
public class MessageTests {
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
    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private final Map<String, Object> consequence = new HashMap<>();

    @Mock
    Core mockCore;
    @Mock
    Activity mockActivity;
    @Mock
    App mockApp;
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
    @Mock
    MessagingDelegate mockMessagingDelegate;
    @Mock
    WebView mockWebView;
    @Mock
    Application mockApplication;
    @Mock
    Looper mockLooper;
    @Mock
    Handler mockHandler;
    @Captor
    ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor;
    @Captor
    ArgumentCaptor<String> interactionArgumentCaptor;
    private Message message;
    private AEPMessage aepMessage;
    private EventHub eventHub;

    @Before
    public void setup() throws Exception {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupDetailsAndConsequenceMaps(setupXdmMap(new MessageTestConfig()));

        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }
    }

    void setupMocks() throws Exception {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(ServiceProvider.class);

        when(mockApp.getCurrentActivity()).thenReturn(mockActivity);
        when(App.getInstance()).thenReturn(mockApp);
        when(MobileCore.getCore()).thenReturn(mockCore);
        when(MobileCore.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getMainLooper()).thenReturn(mockLooper);
        when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
        when(mockServiceProvider.getMessageDelegate()).thenReturn(mockMessagingDelegate);
        when(mockMessagingDelegate.shouldShowMessage(any(FullscreenMessage.class))).thenReturn(true);
        when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
        // Actually run the runnable - mocking the handler.post()
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }
        }).when(mockHandler).post(any(Runnable.class));
        when(mockUIService.createFullscreenMessage(any(String.class), any(MessageSettings.class), any(Map.class))).thenReturn(mockAEPMessage);
        when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        when(mockAEPMessageSettings.getParent()).thenReturn(mockMessage);
        when(mockMessagingState.getExperienceEventDatasetId()).thenReturn("datasetId");
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

    Map<String, Object> setupDetailsAndConsequenceMaps(Map<String, Object> xdmMap) {
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdmMap);
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, html);
        consequence.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        return consequence;
    }

    MessageSettings getMessageSettings(Map<String, Object> rawSettings) {
        MessageSettings messageSettings = null;
        AEPMessageSettings.Builder builder = new AEPMessageSettings.Builder(this);
        Method addMessageSettings = Whitebox.getMethod(Message.class, "addMessageSettings", AEPMessageSettings.Builder.class, Map.class);
        try {
            addMessageSettings.setAccessible(true);
            messageSettings = (MessageSettings) addMessageSettings.invoke(message, builder, rawSettings);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            fail(e.getMessage());
        }
        return messageSettings;
    }

    // ========================================================================================
    // Message constructor tests
    // ========================================================================================
    @Test
    public void test_messageConstructor() throws MessageRequiredFieldMissingException {
        // test
        message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());

        // verify
        assertNotNull(message);
    }

    @Test (expected = MessageRequiredFieldMissingException.class)
    public void test_messageConstructor_MissingDetails() throws MessageRequiredFieldMissingException {
        // setup
        Map<String, Object> consequenceMap = setupDetailsAndConsequenceMaps(setupXdmMap(new MessageTestConfig()));
        consequenceMap.remove(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
        // test
        message = new Message(mockMessagingInternal, consequenceMap, new HashMap<String, Object>(), new HashMap<String, String>());
    }

    @Test (expected = MessageRequiredFieldMissingException.class)
    public void test_messageConstructor_MissingMessageExecutionId() throws MessageRequiredFieldMissingException {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.isMissingMessageId = true;
        Map<String, Object> xdmMap = setupXdmMap(config);
        Map<String, Object> consequenceMap = setupDetailsAndConsequenceMaps(xdmMap);
        // test
        message = new Message(mockMessagingInternal, consequenceMap, new HashMap<String, Object>(), new HashMap<String, String>());
    }

    @Test
    public void test_addMessageSettings_NullMessageSettingsGivesDefaultValues() {
        // test
        MessageSettings settings = getMessageSettings(Collections.EMPTY_MAP);

        // verify
        assertEquals("#FFFFFF", settings.getBackdropColor());
        assertEquals(MessageAnimation.NONE, settings.getDisplayAnimation());
        assertEquals(0, settings.getVerticalInset());
        assertEquals(0, settings.getCornerRadius(), 0.1f);
        assertEquals(100, settings.getHeight());
        assertEquals(100, settings.getWidth());
        assertEquals(MessageAlignment.CENTER, settings.getVerticalAlign());
        assertEquals(MessageAlignment.CENTER, settings.getHorizontalAlign());
        assertEquals(0, settings.getHorizontalInset());
        assertEquals(MessageAnimation.NONE, settings.getDismissAnimation());
        assertEquals(true, settings.getUITakeover());
        assertEquals(Collections.EMPTY_MAP, settings.getGestures());
    }

    @Test
    public void test_addMessageSettings_MessageSettingsContainsNonDefaultValues() {
        // setup
        final Map<String, String> gestureStringMap = new HashMap() {
            {
                put("backgroundTap", "center");
                put("swipeDown", "bottom");
                put("swipeLeft", "left");
                put("swipeRight", "right");
                put("swipeUp", "top");
            }
        };
        Map<String, Object> messageSettings = new HashMap() {
            {
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.SCHEMA_VERSION, "version");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.BACKDROP_COLOR, "#FF5733");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.BACKDROP_OPACITY, 0.5);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.DISPLAY_ANIMATION, "none");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.VERTICAL_INSET, 10);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.CORNER_RADIUS, 25);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.HEIGHT, 70);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.WIDTH, 80);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.VERTICAL_ALIGN, "top");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_ALIGN, "left");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_INSET, 0);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.DISMISS_ANIMATION, "fade");
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.UI_TAKEOVER, false);
                put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.GESTURES, gestureStringMap);
            }
        };

        // test
        MessageSettings settings = getMessageSettings(messageSettings);

        // verify
        assertEquals("#FF5733", settings.getBackdropColor());
        assertEquals(MessageAnimation.NONE, settings.getDisplayAnimation());
        assertEquals(10, settings.getVerticalInset());
        assertEquals(25.0f, settings.getCornerRadius(), 0.1f);
        assertEquals(70, settings.getHeight());
        assertEquals(80, settings.getWidth());
        assertEquals(MessageAlignment.TOP, settings.getVerticalAlign());
        assertEquals(MessageAlignment.LEFT, settings.getHorizontalAlign());
        assertEquals(0, settings.getHorizontalInset());
        assertEquals(MessageAnimation.FADE, settings.getDismissAnimation());
        assertEquals(false, settings.getUITakeover());
        Map<MessageGesture, String>  expectedGestureStringMap = new HashMap() {
            {
                put(MessageGesture.BACKGROUND_TAP, "center");
                put(MessageGesture.SWIPE_DOWN, "bottom");
                put(MessageGesture.SWIPE_LEFT, "left");
                put(MessageGesture.SWIPE_RIGHT, "right");
                put(MessageGesture.SWIPE_UP, "top");
            }
        };
        assertEquals(expectedGestureStringMap, settings.getGestures());
    }

    // ========================================================================================
    // Message show and dismiss tests
    // ========================================================================================
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
        CustomMessagingDelegate customMessageDelegate = new CustomMessagingDelegate();
        when(mockServiceProvider.getMessageDelegate()).thenReturn(customMessageDelegate);

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
        when(mockAEPMessageSettings.getParent()).thenReturn(message);
        // setup custom delegate
        CustomMessagingDelegate customMessageDelegate = new CustomMessagingDelegate();
        customMessageDelegate.setShowMessage(false);
        when(mockServiceProvider.getMessageDelegate()).thenReturn(customMessageDelegate);
        // setup mocks
        try {
            aepMessage = new AEPMessage("html", mockAEPMessageSettings, Collections.<String, String>emptyMap());
        } catch (MessageCreationException e) {
            fail(e.getLocalizedMessage());
        }
        when(mockUIService.createFullscreenMessage(any(String.class), any(MessageSettings.class), any(Map.class))).thenReturn(aepMessage);
        try {
            message = new Message(mockMessagingInternal, consequence, new HashMap<String, Object>(), new HashMap<String, String>());
        } catch (MessageRequiredFieldMissingException e) {
            fail(e.getLocalizedMessage());
        }

        // test
        message.show();

        // expect 1 tracking event: custom delegate suppressed message tracking
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        List<MessagingEdgeEventType> capturedEvents = messagingEdgeEventTypeArgumentCaptor.getAllValues();
        List<String> capturedInteractions = interactionArgumentCaptor.getAllValues();
        // verify custom delegate suppressed message tracking event
        MessagingEdgeEventType displayTrackingEvent = capturedEvents.get(0);
        String interaction = capturedInteractions.get(0);
        assertEquals(MessagingEdgeEventType.IN_APP_DISPLAY, displayTrackingEvent);
        assertEquals("suppressed", interaction);
    }

    @Test
    public void test_messageDismiss_suppressAutoTrackFalse() {
        // test
        message.dismiss(false);

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
    public void test_messageDismiss_suppressAutoTrackTrue() {
        // test
        message.dismiss(true);

        // verify aepMessage dismiss called
        verify(mockAEPMessage, times(1)).dismiss();

        // verify no dismissed tracking event
        verify(mockMessagingInternal, times(0)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
    }

    // ========================================================================================
    // Message overrideUrlLoad tests
    // ========================================================================================
    @Test
    public void test_overrideUrlLoad() {
        // setup
        message.messagingInternal = mockMessagingInternal;
        when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        when(mockAEPMessageSettings.getParent()).thenReturn(message);

        // test
        message.overrideUrlLoad(mockAEPMessage, "adbinapp://dismiss?interaction=deeplinkclicked&link=https://adobe.com");

        // expect 1 event: deeplink click tracking
        verify(mockMessagingInternal, times(1)).handleInAppTrackingInfo(messagingEdgeEventTypeArgumentCaptor.capture(), interactionArgumentCaptor.capture(), any(Message.class));
        List<MessagingEdgeEventType> capturedEvents = messagingEdgeEventTypeArgumentCaptor.getAllValues();
        List<String> capturedInteractions = interactionArgumentCaptor.getAllValues();
        // verify interact tracking event
        MessagingEdgeEventType displayTrackingEvent = capturedEvents.get(0);
        String interaction = capturedInteractions.get(0);
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, displayTrackingEvent);
        assertEquals("deeplinkclicked", interaction);

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

    // ========================================================================================
    // Message track tests
    // ========================================================================================
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

    // ========================================================================================
    // Message javascript handling and loading tests
    // ========================================================================================
    @Test
    public void test_handleJavascript() throws Exception {
        // setup
        Whitebox.setInternalState(message, "webViewHandler", mockHandler);
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        Whitebox.setInternalState(message, "webView", mockWebView);

        // test
        message.handleJavascriptMessage("test", callback);
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");
        // verify evaluate javascript called
        verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_handleJavascript_withNoCallback() throws Exception {
        // setup
        Whitebox.setInternalState(message, "webViewHandler", mockHandler);
        Whitebox.setInternalState(message, "webView", mockWebView);

        // test
        message.handleJavascriptMessage("test", null);
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");
        // verify adobe callback in the script handler is null
        Map<String, Object> scriptHandlers = Whitebox.getInternalState(message, "scriptHandlers");
        WebViewJavascriptInterface webViewJavascriptInterface = (WebViewJavascriptInterface) scriptHandlers.get("test");
        AdobeCallback<String> callback = Whitebox.getInternalState(webViewJavascriptInterface, "callback");
        assertNull(callback);
        // verify evaluate javascript called
        verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_handleJavascript_withNoMessageName() throws Exception {
        // setup
        Whitebox.setInternalState(message, "webViewHandler", mockHandler);
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        Whitebox.setInternalState(message, "webView", mockWebView);

        // test
        message.handleJavascriptMessage(null, callback);
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");
        // verify evaluate javascript not called
        verify(mockWebView, times(0)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_handleJavascript_withDuplicateMessageNames_thenOnlyOneWebViewJavascriptInterfaceCreated() throws Exception {
        // setup
        Whitebox.setInternalState(message, "webViewHandler", mockHandler);
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        AdobeCallback<String> callback2 = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        Whitebox.setInternalState(message, "webView", mockWebView);

        // test
        message.handleJavascriptMessage("test", callback);
        message.handleJavascriptMessage("test", callback2);
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");
        // verify only one WebViewJavascriptInterface created
        Map<String, Object> scriptHandlers = Whitebox.getInternalState(message, "scriptHandlers");
        assertEquals(1, scriptHandlers.size());
        WebViewJavascriptInterface webViewJavascriptInterface = (WebViewJavascriptInterface) scriptHandlers.get("test");
        assertEquals(callback, Whitebox.getInternalState(webViewJavascriptInterface, "callback"));
        // verify evaluate javascript called
        verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_loadJavascript_WithScriptHandlersSet() throws Exception {
        // setup
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        Whitebox.setInternalState(message, "webView", mockWebView);
        // set scriptHandlers map
        Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();
        scriptHandlers.put("test", new WebViewJavascriptInterface(callback));
        Whitebox.setInternalState(message, "scriptHandlers", scriptHandlers);
        // test
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");
        // verify evaluate javascript called
        verify(mockWebView, times(1)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_loadJavascript_WithNoScriptHandlersSet() {
        // setup
        Whitebox.setInternalState(message, "webView", mockWebView);
        // test
        message.evaluateJavascript("(function test(hello world) { return(arg); })()");

        // verify evaluate javascript called
        verify(mockWebView, times(0)).evaluateJavascript(ArgumentMatchers.contains("(function test(hello world) { return(arg); })()"), any(ValueCallback.class));
    }

    @Test
    public void test_loadJavascriptWhenJavascriptIsNull() throws Exception {
        // setup
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        Whitebox.setInternalState(message, "webView", mockWebView);
        // set scriptHandlers map
        Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();
        scriptHandlers.put("test", new WebViewJavascriptInterface(callback));
        Whitebox.setInternalState(message, "scriptHandlers", scriptHandlers);
        // test
        message.evaluateJavascript(null);

        // verify evaluate javascript not called
        verify(mockWebView, times(0)).evaluateJavascript(anyString(), any(ValueCallback.class));
    }
}
