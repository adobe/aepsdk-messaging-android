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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagingInternal.class, ExtensionApi.class})
public class ListenerHubSharedStateTests {
    @Mock
    MessagingInternal mockMessagingInternal;

    @Mock
    ExtensionApi mockExtensionApi;

    private ListenerHubSharedState listenerHubSharedState;
    private int EXECUTOR_TIMEOUT = 5;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Before
    public void beforeEach() {
        listenerHubSharedState = new ListenerHubSharedState(mockExtensionApi,
                EventType.HUB.getName(), EventSource.SHARED_STATE.getName());
        when(mockMessagingInternal.getExecutor()).thenReturn(executor);
        when(mockExtensionApi.getExtension()).thenReturn(mockMessagingInternal);
    }

    @Test
    public void testHear_WhenHubSharedStateEvent() {
        // setup
        EventData eventData = new EventData();
        Event mockEvent = new Event.Builder("testEvent", "test source", "test type").setData(eventData).build();

        // test
        listenerHubSharedState.hear(mockEvent);
        TestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(1)).processHubSharedState(mockEvent);
    }

    @Test
    public void testHear_WithNullEventData() {
        // setup
        Event mockEvent = new Event.Builder("testEvent", EventType.HUB,
                EventSource.SHARED_STATE).setData(null).build();

        // test
        listenerHubSharedState.hear(mockEvent);
        TestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(0)).processHubSharedState(mockEvent);
    }

    @Test
    public void testHear_WithNullParentExtension() {
        // setup
        EventData eventData = new EventData();
        Event mockEvent = new Event.Builder("testEvent", EventType.HUB,
                EventSource.SHARED_STATE).setData(eventData).build();
        when(mockExtensionApi.getExtension()).thenReturn(null);

        // test
        listenerHubSharedState.hear(mockEvent);
        TestUtils.waitForExecutor(executor, EXECUTOR_TIMEOUT);

        // verify
        verify(mockMessagingInternal, times(0)).processHubSharedState(mockEvent);
    }
}