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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Executors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, MobileCore.class, Event.class, App.class, MessagingDelegate.class, ServiceProvider.class})
public class MessagingDelegateTests {

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
    ServiceProvider mockServiceProvider;
    @Mock
    UIService mockUIService;
    @Mock
    MessagingInternal mockMessagingInternal;
    @Captor
    ArgumentCaptor<String> urlStringCaptor;
    private EventHub eventHub;
    private MessagingDelegate messagingDelegate;

    @Before
    public void setup() throws Exception {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();

        messagingDelegate = new MessagingDelegate();
    }

    void setupMocks() throws Exception {
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
        when(MobileCore.getCore()).thenReturn(mockCore);
        when(MobileCore.getApplication()).thenReturn(mockApplication);
        when(mockMessagingInternal.getExecutor()).thenReturn(Executors.newSingleThreadExecutor());
    }

    @Test
    public void test_onMessageShow() {
        // test
        messagingDelegate.onShow(mockAEPMessage);

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());
    }

    @Test
    public void test_onMessageDismiss() {
        // test
        messagingDelegate.onDismiss(mockAEPMessage);

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());
    }

    @Test
    public void test_onMessageShowFailure() {
        // test
        messagingDelegate.onShowFailure();

        // verify Log.debug called
        verifyStatic(Log.class);
        Log.debug(anyString(), anyString(), any());
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // test
        messagingDelegate.openUrl(mockAEPMessage, "adb_deeplink://signup");

        // verify the internal open url method is called
        verify(mockAEPMessage, times(1)).openUrl(urlStringCaptor.capture());
        assertEquals("adb_deeplink://signup", urlStringCaptor.getValue());
    }

    @Test
    public void test_openUrlWithWebLink() {
        // test
        messagingDelegate.openUrl(mockAEPMessage, "https://www.adobe.com");

        // verify the ui service is called to show the url
        verify(mockUIService, times(1)).showUrl(urlStringCaptor.capture());
        assertEquals("https://www.adobe.com", urlStringCaptor.getValue());
    }

    @Test
    public void test_openUrlWithInvalidLink() {
        // test
        messagingDelegate.openUrl(mockAEPMessage, "htp://www.adobe.com");

        // verify no internal open url or ui service show url method is called
        verify(mockUIService, times(0)).showUrl(anyString());
        verify(mockAEPMessage, times(0)).openUrl(anyString());
    }
}
