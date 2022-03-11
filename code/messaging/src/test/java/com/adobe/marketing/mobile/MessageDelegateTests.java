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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.app.Application;
import android.content.Context;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, MobileCore.class, Event.class, App.class, MessageDelegate.class, ServiceProvider.class})
public class MessageDelegateTests {

    private EventHub eventHub;
    private MessageDelegate messageDelegate;

    @Mock
    Core mockCore;
    @Mock
    AEPMessage mockAEPMessage;
    @Mock
    AEPMessageSettings mockAEPMessageSettings;
    @Mock
    Message mockMessage;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    Application mockApplication;
    @Mock
    Context context;
    @Mock
    WebSettings mockWebSettings;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    UIService mockUIService;
    @Mock
    WebView mockWebView;
    @Mock
    MessagingInternal mockMessagingInternal;
    @Captor
    ArgumentCaptor<ValueCallback<String>> valueCallbackArgumentCaptor;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(Log.class);
        PowerMockito.mockStatic(ServiceProvider.class);
        when(mockAEPMessage.getSettings()).thenReturn(mockAEPMessageSettings);
        when(mockAEPMessageSettings.getParent()).thenReturn(mockMessage);
        when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
        when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
        when(mockApplication.getApplicationContext()).thenReturn(context);

        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        when(MobileCore.getCore()).thenReturn(mockCore);
        when(MobileCore.getApplication()).thenReturn(mockApplication);

        mockWebView = PowerMockito.mock(WebView.class);
        PowerMockito.whenNew(WebView.class).withAnyArguments().thenReturn(mockWebView);
        when(mockWebView.getSettings()).thenReturn(mockWebSettings);
        when(mockMessagingInternal.getExecutor()).thenReturn(Executors.newSingleThreadExecutor());

        messageDelegate = new MessageDelegate();
    }

    @Test
    public void test_onMessageShow() {
        // test
        messageDelegate.onShow(mockAEPMessage);

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());
    }

    @Test
    public void test_onMessageDismiss() {
        // test
        messageDelegate.onDismiss(mockAEPMessage);

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());

        // verify Message.dismiss() called 1 time
        verify(mockMessage, times(1)).dismiss();
    }

    @Test
    public void test_onMessageShowFailure() {
        // test
        messageDelegate.onShowFailure();

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // test
        messageDelegate.openUrl(mockAEPMessage, "adb_deeplink://signup");

        // verify the internal open url method is called
        verify(mockAEPMessage, times(1)).openUrl(anyString());
    }

    @Test
    public void test_openUrlWithWebLink() {
        // test
        messageDelegate.openUrl(mockAEPMessage, "https://www.adobe.com");

        // verify the ui service is called to show the url
        verify(mockUIService, times(1)).showUrl(anyString());
    }

    @Test
    public void test_openUrlWithInvalidLink() {
        // test
        messageDelegate.openUrl(mockAEPMessage, "");

        // verify no internal open url or ui service show url method is called
        verify(mockUIService, times(0)).showUrl(anyString());
        verify(mockAEPMessage, times(0)).openUrl(anyString());
    }

    @Test
    public void test_loadJavascript_WithScriptHandlersSet() {
        // setup
        AdobeCallback<String> callback = new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                Assert.assertEquals("hello world", s);
            }
        };
        // set js webview
        Whitebox.setInternalState(MessageDelegate.class, "jsWebView", new WebView(context));
        // set scriptHandlers map
        Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();
        scriptHandlers.put("test", new WebViewJavascriptInterface(callback));
        Whitebox.setInternalState(MessageDelegate.class, "scriptHandlers", scriptHandlers);

        // test
        messageDelegate.evaluateJavascript("function test(hello world) { print(arg); }");
        // verify evaluate javascript called
        verify(mockWebView, times(1)).evaluateJavascript(anyString(), valueCallbackArgumentCaptor.capture());
        ValueCallback<String> valueCallback = valueCallbackArgumentCaptor.getValue();
        valueCallback.onReceiveValue("hello world");
    }

    @Test
    public void test_loadJavascript_WithNoScriptHandlersSet() {
        // test
        messageDelegate.evaluateJavascript("function test(hello world) { print(arg); }");

        // verify evaluate javascript called
        verify(mockWebView, times(0)).evaluateJavascript(anyString(), any(ValueCallback.class));
    }

    @Test
    public void test_loadJavascriptWhenJavascriptIsNull() throws Exception {
        // setup
        WebView mockWebview = Mockito.mock(WebView.class);
        PowerMockito.whenNew(WebView.class).withAnyArguments().thenReturn(mockWebview);
        PowerMockito.when(mockWebview.getSettings()).thenReturn(mockWebSettings);
        // test
        messageDelegate.evaluateJavascript(null);

        // verify evaluate javascript not called
        verify(mockWebview, times(0)).evaluateJavascript(anyString(), any(ValueCallback.class));
    }
}
