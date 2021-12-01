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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalHandleRulesResponseContentTests {
    private final Map<String, Object> mobileParameters = new HashMap<String, Object>() {
        {
            put(MessagingConstants.EventDataKeys.MobileParametersKeys.SCHEMA_VERSION, "version");
            put(MessagingConstants.EventDataKeys.MobileParametersKeys.VERTICAL_ALIGN, "center");
            put(MessagingConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_INSET, 0);
            put(MessagingConstants.EventDataKeys.MobileParametersKeys.DISMISS_ANIMATION, "bottom");
            put(MessagingConstants.EventDataKeys.MobileParametersKeys.UI_TAKEOVER, true);
        }
    };
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    private final byte[] base64EncodedBytes = "decisionScope".getBytes(StandardCharsets.UTF_8);
    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Application mockApplication;
    @Mock
    Context context;
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    AndroidEncodingService mockAndroidEncodingService;
    @Mock
    UIService mockUIService;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;
    @Mock
    AEPMessage mockAEPMessage;
    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        when(MobileCore.getCore()).thenReturn(mockCore);

        // setup activity id mocks
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(context);
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "metaData", bundle);
        when(bundle.getString(anyString())).thenReturn("mock_activity");
        // setup placement id mocks
        when(mockApplication.getPackageName()).thenReturn("mock_placement");

        // setup services mocks
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockAndroidEncodingService.base64Encode(any(byte[].class))).thenReturn(base64EncodedBytes);

        // setup createFullscreenMessage mock
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(UIService.MessageSettings.class))).thenReturn(mockAEPMessage);

        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "mock_dataset_id");

        // setup identity shared state mock
        when(mockExtensionApi.getXDMSharedEventState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        ecidMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
        ids.add(ecidMap);
        identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ECID, ids);
        mockIdentityState.put(MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        messagingInternal = new MessagingInternal(mockExtensionApi);
        MobileCore.setApplication(App.getApplication());
    }

    // ========================================================================================
    // handling rules response events
    // ========================================================================================
    @Test
    public void test_handleRulesResponseEvent_ValidConsequencePresent() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a valid rules response content event with a triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is called
        verify(mockAEPMessage, times(1)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_UnsupportedConsequenceType() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, "unknownIamType");
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_MissingConsequenceType() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    public void test_handleRulesResponseEvent_NullConsequences() {
        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, null);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyConsequences() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_EmptyDetails() {
        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_NullDetails() {
        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, null);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyHTML() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingHTML() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingMessageId() {
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockAEPMessage, times(0)).show();
    }
}
