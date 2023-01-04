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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import com.adobe.marketing.mobile.LoggingMode;
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

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingDelegateTests {

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
    Message mockMessage;

    @Captor
    ArgumentCaptor<String> urlStringCaptor;

    private MessagingDelegate messagingDelegate;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        messagingDelegate = new MessagingDelegate();
        Log.setLogLevel(LoggingMode.VERBOSE);
    }

    @After
    public void tearDown() {
        reset(mockFullscreenMessage);
        reset(mockServiceProvider);
        reset(mockLogging);
        reset(mockUIService);
        reset(mockMessageSettings);
        reset(mockMessage);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getLoggingService()).thenReturn(mockLogging);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);

            runnable.run();
        }
    }

    @Test
    public void test_onMessageShow() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // test
            messagingDelegate.onShow(mockFullscreenMessage);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));
        }
    }

    @Test
    public void test_onMessageDismiss() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // test
            messagingDelegate.onDismiss(mockFullscreenMessage);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));
        }
    }

    @Test
    public void test_onMessageShowFailure() {
        try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
            // test
            messagingDelegate.onShowFailure();

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));
        }
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // test
        messagingDelegate.openUrl(mockFullscreenMessage, "adb_deeplink://signup");

        // verify the internal open url method is called
        verify(mockFullscreenMessage, times(1)).openUrl(urlStringCaptor.capture());
        assertEquals("adb_deeplink://signup", urlStringCaptor.getValue());
    }

    @Test
    public void test_openUrlWithWebLink() {
        runWithMockedServiceProvider(() -> {
            // test
            messagingDelegate.openUrl(mockFullscreenMessage, "https://www.adobe.com");

            // verify the ui service is called to show the url
            verify(mockUIService, times(1)).showUrl(urlStringCaptor.capture());
            assertEquals("https://www.adobe.com", urlStringCaptor.getValue());
        });
    }

    @Test
    public void test_openUrlWithInvalidLink() {
        runWithMockedServiceProvider(() -> {
            // test
            messagingDelegate.openUrl(mockFullscreenMessage, "htp://www.adobe.com");

            // verify no internal open url or ui service show url method is called
            verify(mockUIService, times(0)).showUrl(anyString());
            verify(mockFullscreenMessage, times(0)).openUrl(anyString());
        });
    }

    @Test
    public void test_openUrlWithNullUrl() {
        runWithMockedServiceProvider(() -> {
            // test
            messagingDelegate.openUrl(mockFullscreenMessage, null);

            // verify no internal open url method is called
            verify(mockFullscreenMessage, times(0)).openUrl(urlStringCaptor.capture());
        });
    }

    @Test
    public void test_overrideUrlLoadWithInvalidUri() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(mockMessage);

        // test
        messagingDelegate.overrideUrlLoad(mockFullscreenMessage, "invaliduri");

        // verify no message tracking call and message settings weren't created
        verify(mockMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));
        verify(mockMessageSettings, times(0)).getParent();
    }

    @Test
    public void test_overrideUrlLoadWithInvalidScheme() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(mockMessage);

        // test
        messagingDelegate.overrideUrlLoad(mockFullscreenMessage, "notadbinapp://");

        // verify no message tracking call and message settings weren't created
        verify(mockMessage, times(0)).track(anyString(), any(MessagingEdgeEventType.class));
        verify(mockMessageSettings, times(0)).getParent();
    }

    @Test
    public void test_overrideUrlLoadWithJavascriptPayload() {
        // setup
        when(mockFullscreenMessage.getMessageSettings()).thenReturn(mockMessageSettings);
        when(mockMessageSettings.getParent()).thenReturn(mockMessage);

        // test
        messagingDelegate.overrideUrlLoad(mockFullscreenMessage, "adbinapp://dismiss?js=(function test() { return 'javascript value'; })();");

        // verify encoded javascript evaluated by the webview object
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockMessage, times(1)).evaluateJavascript(stringArgumentCaptor.capture());
        assertEquals("(function test() { return 'javascript value'; })();", stringArgumentCaptor.getValue());
        verify(mockMessage, times(1)).dismiss(anyBoolean());
        verify(mockMessageSettings, times(1)).getParent();
    }
}
