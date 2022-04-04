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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagingInternal.class, ExtensionApi.class})
public class ListenerMessagingRequestContentTests {
    private final int EXECUTOR_TIMEOUT = 5; // in seconds
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Mock
    MessagingInternal mockMessagingInternal;
    @Mock
    ExtensionApi mockExtensionApi;
    private ListenerMessagingRequestContent listenerMessagingRequestContent;

    @Before
    public void beforeEach() {
        listenerMessagingRequestContent = new ListenerMessagingRequestContent(mockExtensionApi,
                MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName());
        when(mockMessagingInternal.getExecutor()).thenReturn(executor);
        when(mockExtensionApi.getExtension()).thenReturn(mockMessagingInternal);
    }

    @Test
    public void testHear_WhenMessagingRequestContentEvent() {
        // setup
        EventData eventData = new EventData();
        Event mockEvent = new Event.Builder("testEvent", "test source", "test type").setData(eventData).build();

        // test
        listenerMessagingRequestContent.hear(mockEvent);
        MessagingTestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(1)).queueEvent(mockEvent);
        verify(mockMessagingInternal, times(1)).processEvents();
    }

    @Test
    public void testHear_WithNullEventData() {
        // setup
        Event mockEvent = new Event.Builder("testEvent", MessagingConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName()).setData(null).build();

        // test
        listenerMessagingRequestContent.hear(mockEvent);
        MessagingTestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(0)).queueEvent(mockEvent);
        verify(mockMessagingInternal, times(0)).processEvents();
    }

    @Test
    public void testHear_WithNullParentExtension() {
        // setup
        EventData eventData = new EventData();
        Event mockEvent = new Event.Builder("testEvent", MessagingConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName()).setData(eventData).build();
        when(mockExtensionApi.getExtension()).thenReturn(null);

        // test
        listenerMessagingRequestContent.hear(mockEvent);
        MessagingTestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(0)).queueEvent(mockEvent);
        verify(mockMessagingInternal, times(0)).processEvents();
    }
}
