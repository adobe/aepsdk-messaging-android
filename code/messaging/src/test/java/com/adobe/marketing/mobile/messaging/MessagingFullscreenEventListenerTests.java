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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.WebViewJavascriptInterface;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.Logging;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventHandler;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageSettings;
import com.adobe.marketing.mobile.services.uri.UriOpening;

import org.junit.After;
import org.junit.Assert;
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
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingFullscreenEventListenerTests {

    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    Logging mockLogging;
    @Mock
    UIService mockUIService;
    @Mock
    UriOpening mockUriOpening;
    @Mock
    PresentableMessageMapper mockPresentableMessageMapper;
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

    @Captor
    ArgumentCaptor<String> urlStringCaptor;

    private MessagingFullscreenEventListener eventListener;
    private Map <String, WebViewJavascriptInterface> scriptHandlerMap;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        try {
            eventListener = new MessagingFullscreenEventListener();
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
        Log.setLogLevel(LoggingMode.VERBOSE);
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockLogging);
        reset(mockUIService);
        reset(mockUriOpening);
        reset(mockPresentableMessageMapper);
        reset(mockMessage);
        reset(mockInAppPresentable);
        reset(mockPresentation);
        reset(mockMessageSettings);
        reset(mockPresentationError);
        reset(mockEventHandler);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class);
             MockedStatic<PresentableMessageMapper> presentableMessageMapperMockedStatic = Mockito.mockStatic(PresentableMessageMapper.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getLoggingService()).thenReturn(mockLogging);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getUriService()).thenReturn(mockUriOpening);
            presentableMessageMapperMockedStatic.when(PresentableMessageMapper::getInstance).thenReturn(mockPresentableMessageMapper);
            when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString())).thenReturn(mockMessage);
            when(mockInAppPresentable.getPresentation()).thenReturn(mockPresentation);
            when(mockPresentation.getId()).thenReturn("mockId");
            runnable.run();
        }
    }

    @Test
    public void test_onMessageShow_withTracking() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockMessage.getAutoTrack()).thenReturn(true);

            // test
            eventListener.onShow(mockInAppPresentable);

            // verify
            verify(mockMessage, times(1)).track(null, MessagingEdgeEventType.IN_APP_DISPLAY);
        });
    }

    @Test
    public void test_onMessageShow_withAutoTrackDisabled() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString())).thenReturn(null);

            // test
            eventListener.onShow(mockInAppPresentable);

            // verify
            verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.IN_APP_DISPLAY);
        });
    }

    @Test
    public void test_onMessageShow_presentableMessageNotInMap() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString())).thenReturn(null);

            // test
            eventListener.onShow(mockInAppPresentable);

            // verify
            verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.IN_APP_DISPLAY);
        });
    }

    @Test
    public void test_onMessageDismiss_withAutoTrack() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockMessage.getAutoTrack()).thenReturn(true);

            // test
            eventListener.onDismiss(mockInAppPresentable);

            // verify
            verify(mockMessage, times(1)).track(null, MessagingEdgeEventType.IN_APP_DISMISS);
        });
    }

    @Test
    public void test_onMessageDismiss_withAutoTrackDisabled() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockMessage.getAutoTrack()).thenReturn(false);

            // test
            eventListener.onDismiss(mockInAppPresentable);

            // verify
            verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.IN_APP_DISMISS);
        });
    }

    @Test
    public void test_onMessageDismiss_presentableMessageNotInMap() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString())).thenReturn(null);

            // test
            eventListener.onDismiss(mockInAppPresentable);

            // verify
            verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.IN_APP_DISMISS);
        });
    }

    @Test
    public void test_onMessageError() {
        runWithMockedServiceProvider(() -> {
            try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                // test
                eventListener.onError(mockInAppPresentable, mockPresentationError);

                // verify
                logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));
            }
        });
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // setup
        runWithMockedServiceProvider(() -> {
            // test
            eventListener.openUrl("adb_deeplink://signup");

            // verify the ui service is called to handle the deeplink
            verify(mockUriOpening, times(1)).openUri(urlStringCaptor.capture());
            assertEquals("adb_deeplink://signup", urlStringCaptor.getValue());
        });
    }

    @Test
    public void test_openUrlWithWebLink() {
        runWithMockedServiceProvider(() -> {
            // test
            eventListener.openUrl("https://www.adobe.com");

            // verify the ui service is called to show the url
            verify(mockUriOpening, times(1)).openUri(urlStringCaptor.capture());
            assertEquals("https://www.adobe.com", urlStringCaptor.getValue());
        });
    }

    @Test
    public void test_openUrlWithNullUrl() {
        runWithMockedServiceProvider(() -> {
            // test
            eventListener.openUrl(null);

            // verify no internal open url method is called
            verify(mockUriOpening, times(0)).openUri(urlStringCaptor.capture());
        });
    }

    @Test
    public void test_onUrlLoading_nullUrlString() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(((InAppMessage) mockPresentation).getSettings()).thenReturn(mockMessageSettings);

            // test
            boolean result = eventListener.onUrlLoading(mockInAppPresentable, null);

            // verify no message tracking call
            assertTrue(result);
            verify(mockPresentableMessageMapper, times(0)).getMessageFromPresentableId(anyString());
            verify(mockMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));
        });
    }

    @Test
    public void test_onUrlLoading_withInvalidUri() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(((InAppMessage) mockPresentation).getSettings()).thenReturn(mockMessageSettings);

            // test
            boolean result = eventListener.onUrlLoading(mockInAppPresentable, "{invalid}");

            // verify no message tracking call
            assertTrue(result);
            verify(mockPresentableMessageMapper, times(0)).getMessageFromPresentableId(anyString());
            verify(mockMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));
        });
    }

    @Test
    public void test_onUrlLoading_withInvalidScheme() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(((InAppMessage) mockPresentation).getSettings()).thenReturn(mockMessageSettings);

            // test
            boolean result = eventListener.onUrlLoading(mockInAppPresentable, "notadbinapp://com.adobe.com");

            // verify no message tracking call and message settings weren't created
            Assert.assertFalse(result);
            verify(mockMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));
            verify(mockPresentableMessageMapper, times(0)).getMessageFromPresentableId(anyString());
        });
    }

    @Test
    public void test_onUrlLoading_withJavascriptPayload() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(((InAppMessage) mockPresentation).getSettings()).thenReturn(mockMessageSettings);
            when(((InAppMessage) mockPresentation).getEventHandler()).thenReturn(mockEventHandler);

            // test
            eventListener.onUrlLoading(mockInAppPresentable, "adbinapp://dismiss?interaction=javascript&link=js%3D%28function%28%29+%7B+return+%27javascript+value%27%3B+%7D%29%28%29%3B");

            // verify no message tracking call and message settings weren't created
            verify(mockMessage, times(1)).track(eq("javascript"), eq(MessagingEdgeEventType.IN_APP_INTERACT));
            ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockEventHandler, times(1)).evaluateJavascript(stringArgumentCaptor.capture(), any());
            assertEquals("js=(function() { return 'javascript value'; })();", stringArgumentCaptor.getValue());
        });
    }

    @Test
    public void test_onUrlLoading_withDeeplink() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(((InAppMessage) mockPresentation).getSettings()).thenReturn(mockMessageSettings);
            when(((InAppMessage) mockPresentation).getEventHandler()).thenReturn(mockEventHandler);

            // test
            eventListener.onUrlLoading(mockInAppPresentable, "adbinapp://dismiss?interaction=deeplink&link=scheme%3A%2F%2Fparameters%3Fparam1%3Dvalue1%26param2%3Dvalue2");

            // verify no message tracking call and message settings weren't created
            verify(mockMessage, times(0)).track(eq("deeplink"), eq(MessagingEdgeEventType.IN_APP_DISMISS));
            ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockUriOpening, times(1)).openUri(stringArgumentCaptor.capture());
            assertEquals("scheme://parameters?param1=value1&param2=value2", stringArgumentCaptor.getValue());
        });
    }
}
