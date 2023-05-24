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

package com.adobe.marketing.mobile.messaging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.Logging;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
public class MessagingFullscreenMessageDelegateTests {

    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    Logging mockLogging;
    @Mock
    UIService mockUIService;
    @Mock
    FullscreenMessage mockFullscreenMessage;
    @Mock
    MessageSettings mockMessageSettings;
    @Mock
    MessagingExtension mockMessagingExtension;
    @Mock
    WebView mockWebView;
    @Mock
    Handler mockWebViewHandler;

    @Captor
    ArgumentCaptor<String> urlStringCaptor;

    private InternalMessage internalMessage;
    private Map<String, WebViewJavascriptInterface> scriptHandlerMap;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        try {
            internalMessage = new InternalMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), mockWebView, mockWebViewHandler, new HashMap<>());
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
        Log.setLogLevel(LoggingMode.VERBOSE);
    }

    @After
    public void tearDown() {
        reset(mockFullscreenMessage);
        reset(mockServiceProvider);
        reset(mockLogging);
        reset(mockUIService);
        reset(mockMessageSettings);
        reset(mockMessagingExtension);
        reset(mockWebView);
        reset(mockWebViewHandler);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getLoggingService()).thenReturn(mockLogging);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockMessageSettings.getParent()).thenReturn(mockFullscreenMessage);

            runnable.run();
        }
    }

    RuleConsequence createRuleConsequence() {
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "html");
        return new RuleConsequence("123456789", MessagingTestConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);
    }

    @Test
    public void test_onMessageShow_whenMessageAutoTrackIsTrue() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // setup
            ArgumentCaptor<String> logArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
            InternalMessage mockInternalMessage = Mockito.mock(InternalMessage.class);
            when(mockInternalMessage.getAutoTrack()).thenReturn(true);
            when(mockFullscreenMessage.getParent()).thenReturn(mockInternalMessage);

            // test
            internalMessage.onShow(mockFullscreenMessage);

            // verify tracking event data
            verify(mockInternalMessage, times(1)).track(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture());
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISPLAY);
            assertEquals(null, interaction);

            // verify message display logging
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), logArgumentCaptor.capture()), times(1));
            assertEquals("Fullscreen message shown.", logArgumentCaptor.getValue());
        }
    }

    @Test
    public void test_onMessageShow_whenMessageAutoTrackIsFalse() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // setup
            ArgumentCaptor<String> logArgumentCaptor = ArgumentCaptor.forClass(String.class);
            InternalMessage mockInternalMessage = Mockito.mock(InternalMessage.class);
            when(mockInternalMessage.getAutoTrack()).thenReturn(false);
            when(mockFullscreenMessage.getParent()).thenReturn(mockInternalMessage);

            // test
            internalMessage.onShow(mockFullscreenMessage);

            // verify no tracking event
            verify(mockInternalMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));

            // verify message display logging
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), logArgumentCaptor.capture()), times(1));
            assertEquals("Fullscreen message shown.", logArgumentCaptor.getValue());
        }
    }

    @Test
    public void test_onMessageDismiss_whenAutoTrackTrue() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // setup
            ArgumentCaptor<String> logArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
            InternalMessage mockInternalMessage = Mockito.mock(InternalMessage.class);
            when(mockInternalMessage.getAutoTrack()).thenReturn(true);
            when(mockFullscreenMessage.getParent()).thenReturn(mockInternalMessage);

            // test
            internalMessage.onDismiss(mockFullscreenMessage);

            // verify message dismiss logging
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), logArgumentCaptor.capture()), times(1));
            assertEquals("Fullscreen message dismissed.", logArgumentCaptor.getValue());

            // verify tracking event data
            verify(mockInternalMessage, times(1)).track(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture());
            MessagingEdgeEventType eventType = messagingEdgeEventTypeArgumentCaptor.getValue();
            String interaction = interactionArgumentCaptor.getValue();
            assertEquals(eventType, MessagingEdgeEventType.IN_APP_DISMISS);
            assertEquals(null, interaction);
        }
    }

    @Test
    public void test_onMessageDismiss_whenAutoTrackFalse() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // setup
            ArgumentCaptor<String> logArgumentCaptor = ArgumentCaptor.forClass(String.class);
            InternalMessage mockInternalMessage = Mockito.mock(InternalMessage.class);
            when(mockInternalMessage.getAutoTrack()).thenReturn(false);
            when(mockFullscreenMessage.getParent()).thenReturn(mockInternalMessage);

            // test
            internalMessage.onDismiss(mockFullscreenMessage);

            // verify message dismiss logging
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), logArgumentCaptor.capture()), times(1));
            assertEquals("Fullscreen message dismissed.", logArgumentCaptor.getValue());

            // verify tracking event data
            verify(mockInternalMessage, times(0)).track(any(), any());
        }
    }

    @Test
    public void test_onMessageShowFailure() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // setup
            ArgumentCaptor<String> logArgumentCaptor = ArgumentCaptor.forClass(String.class);

            // test
            internalMessage.onShowFailure();

            // verify message failed to show logging
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), logArgumentCaptor.capture()), times(1));
            assertEquals("Fullscreen message failed to show.", logArgumentCaptor.getValue());
        }
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // setup
        runWithMockedServiceProvider(() -> {
            // test
            internalMessage.openUrl("adb_deeplink://signup");

            // verify the ui service is called to handle the deeplink
            verify(mockUIService, times(1)).showUrl(urlStringCaptor.capture());
            assertEquals("adb_deeplink://signup", urlStringCaptor.getValue());
        });
    }

    @Test
    public void test_openUrlWithWebLink() {
        runWithMockedServiceProvider(() -> {
            // test
            internalMessage.openUrl("https://www.adobe.com");

            // verify the ui service is called to show the url
            verify(mockUIService, times(1)).showUrl(urlStringCaptor.capture());
            assertEquals("https://www.adobe.com", urlStringCaptor.getValue());
        });
    }

    @Test
    public void test_openUrlWithNullUrl() {
        runWithMockedServiceProvider(() -> {
            // test
            internalMessage.openUrl(null);

            // verify no internal open url method is called
            verify(mockFullscreenMessage, times(0)).openUrl(urlStringCaptor.capture());
        });
    }

    @Test
    public void test_overrideUrlLoadWithInvalidUri() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(internalMessage);

        // test
        internalMessage.overrideUrlLoad(mockFullscreenMessage, "invaliduri");

        // verify no message tracking call and message settings weren't created
        verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), eq(internalMessage));
        verify(mockMessageSettings, times(0)).getParent();
    }

    @Test
    public void test_overrideUrlLoadWithInvalidScheme() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(internalMessage);

        // test
        internalMessage.overrideUrlLoad(mockFullscreenMessage, "notadbinapp://");

        // verify no message tracking call and message settings weren't created
        verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), eq(internalMessage));
        verify(mockMessageSettings, times(0)).getParent();
    }

    @Test
    public void test_overrideUrlLoad_uRLWithNoQueryParameters() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(internalMessage);

        // test
        internalMessage.overrideUrlLoad(mockFullscreenMessage, "adbinapp://dismiss");

        // verify no message tracking call and message settings weren't created
        verify(mockMessagingExtension, times(0)).sendPropositionInteraction(anyString(), any(MessagingEdgeEventType.class), eq(internalMessage));
        verify(mockMessageSettings, times(1)).getParent();
        // TODO: To verify if that dismiss by mocking FullscreenMessage in internalMessage class
        //verify(mockFullscreenMessage, times(1)).dismiss();
    }

    @Test
    public void test_overrideUrlLoadWithJavascriptPayload() {
        // setup
        scriptHandlerMap = new HashMap<>();
        scriptHandlerMap.put("test", new WebViewJavascriptInterface(s -> assertEquals("javascript value", s)));

        try {
            internalMessage = new InternalMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), mockWebView, mockWebViewHandler, scriptHandlerMap);
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(internalMessage);

        // test
        internalMessage.overrideUrlLoad(mockFullscreenMessage, "adbinapp://dismiss?interaction=javascript&link=js%3D%28function%28%29+%7B+return+%27javascript+value%27%3B+%7D%29%28%29%3B");

        // verify javascript evaluated by the webview object
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWebView, times(1)).evaluateJavascript(stringArgumentCaptor.capture(), any(ValueCallback.class));
        assertEquals("js=(function() { return 'javascript value'; })();", stringArgumentCaptor.getValue());
    }

    @Test
    public void test_overrideUrlLoadWithDeeplink() {
        // setup
        runWithMockedServiceProvider(() -> {
            try {
                internalMessage = new InternalMessage(mockMessagingExtension, createRuleConsequence(), new HashMap<>(), new HashMap<>(), mockWebView, mockWebViewHandler, scriptHandlerMap);
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
            when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
            when(mockMessageSettings.getParent()).thenReturn(internalMessage);

            // test
            internalMessage.overrideUrlLoad(mockFullscreenMessage, "adbinapp://dismiss?interaction=deeplink&link=scheme%3A%2F%2Fparameters%3Fparam1%3Dvalue1%26param2%3Dvalue2");

            // verify deeplink loaded with the uiservice
            ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockUIService, times(1)).showUrl(stringArgumentCaptor.capture());
            assertEquals("scheme://parameters?param1=value1&param2=value2", stringArgumentCaptor.getValue());
        });
    }

    @Test
    public void test_onBackPressed() {
        // setup
        ArgumentCaptor<String> interactionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor = ArgumentCaptor.forClass(MessagingEdgeEventType.class);
        InternalMessage mockInternalMessage = Mockito.mock(InternalMessage.class);
        when(mockFullscreenMessage.getParent()).thenReturn(mockInternalMessage);

        // test
        internalMessage.onBackPressed(mockFullscreenMessage);

        // verify tracking event data
        verify(mockInternalMessage, times(2)).track(interactionArgumentCaptor.capture(), messagingEdgeEventTypeArgumentCaptor.capture());
        List<MessagingEdgeEventType> eventTypeList = messagingEdgeEventTypeArgumentCaptor.getAllValues();
        List<String> interactionList = interactionArgumentCaptor.getAllValues();
        // verify interact event
        String interaction = interactionList.get(0);
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, eventTypeList.get(0));
        assertEquals("backPress", interaction);
        // verify dismiss event
        interaction = interactionList.get(1);
        assertEquals(MessagingEdgeEventType.IN_APP_DISMISS, eventTypeList.get(1));
        assertEquals(null, interaction);
    }

}
