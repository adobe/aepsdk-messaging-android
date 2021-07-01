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

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, App.class})
public class MessagingDelegateTests {

    private MessagingDelegate messagingDelegate;

    @Mock
    Activity mockActivity;
    @Mock
    MessagesMonitor mockMessagesMonitor;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(App.class);
        Mockito.when(App.getCurrentActivity()).thenReturn(mockActivity);
        messagingDelegate = new MessagingDelegate();
    }

    @Test
    public void test_onShow() {
        // setup
        final AndroidFullscreenMessage androidFullscreenMessage = new AndroidFullscreenMessage("html", messagingDelegate, mockMessagesMonitor);
        // test
        androidFullscreenMessage.show();
        // verify
        assertTrue(messagingDelegate.wasLastMessageDisplayed());
    }

    @Test
    public void test_onDismiss() {
        // setup
        final AndroidFullscreenMessage androidFullscreenMessage = new AndroidFullscreenMessage("html", messagingDelegate, mockMessagesMonitor);
        // test
        androidFullscreenMessage.dismissed();
        // verify
        assertTrue(messagingDelegate.wasLastMessageDisplayed());
    }

    @Test
    public void test_onShowFailure() {
        // setup mocks
        when(mockMessagesMonitor.isDisplayed()).thenReturn(true);
        // setup
        final AndroidFullscreenMessage androidFullscreenMessage = new AndroidFullscreenMessage("html", messagingDelegate, mockMessagesMonitor);
        // test
        androidFullscreenMessage.show();
        // verify
        assertFalse(messagingDelegate.wasLastMessageDisplayed());
    }

    @Test
    public void test_overrideUrlLoad() {
        // setup
        final AndroidFullscreenMessage androidFullscreenMessage = new AndroidFullscreenMessage("html", messagingDelegate, mockMessagesMonitor);
        androidFullscreenMessage.messageFullScreenActivity = mockActivity;
        // test
        messagingDelegate.overrideUrlLoad(androidFullscreenMessage, "adbinapp://cancel");
        // verify
        assertTrue(messagingDelegate.wasLastMessageDisplayed());
    }
}
