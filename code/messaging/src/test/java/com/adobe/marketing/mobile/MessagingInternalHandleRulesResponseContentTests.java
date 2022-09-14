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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ServiceProvider.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalHandleRulesResponseContentTests {
    private final static String mockAppId = "mock_applicationId";
    private final Map<String, Object> mobileParameters = new HashMap() {
        {
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.SCHEMA_VERSION, "version");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.VERTICAL_ALIGN, "center");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_INSET, 0);
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.DISMISS_ANIMATION, "bottom");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.UI_TAKEOVER, true);
        }
    };
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Application mockApplication;
    @Mock
    Context mockContext;
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    AndroidEncodingService mockAndroidEncodingService;
    @Mock
    AndroidSystemInfoService mockAndroidSystemInfoService;
    @Mock
    AndroidNetworkService mockAndroidNetworkService;
    @Mock
    UIService mockUIService;
    @Mock
    PackageManager packageManager;
    @Mock
    AEPMessage mockAEPMessage;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    MessagingDelegate mockMessagingDelegate;

    private MessagingInternal messagingInternal;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;

    @Before
    public void setup() throws Exception {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupPlatformServicesMocks();
        setupApplicationIdMocks();
        setupSharedStateMocks();

        messagingInternal = new MessagingInternal(mockExtensionApi);
        MobileCore.setApplication(App.getApplication());
    }

    void setupMocks() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(ServiceProvider.class);
        when(MobileCore.getCore()).thenReturn(mockCore);
        when(MobileCore.getApplication()).thenReturn(mockApplication);
    }

    void setupPlatformServicesMocks() {
        jsonUtilityService = new AndroidJsonUtility();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
        when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
        when(mockServiceProvider.getMessageDelegate()).thenReturn(mockMessagingDelegate);
        when(mockMessagingDelegate.shouldShowMessage(any(FullscreenMessage.class))).thenReturn(true);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);

        final File mockCache = new File("mock_cache");
        when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);

        // setup createFullscreenMessage mock
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(MessageSettings.class), any(Map.class))).thenReturn(mockAEPMessage);
    }

    void setupApplicationIdMocks() {
        when(App.getApplication()).thenReturn(mockApplication);
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn(mockAppId);
        when(mockContext.getPackageName()).thenReturn(mockAppId);
    }

    void setupSharedStateMocks() {
        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "mock_dataset_id");

        // setup identity shared state mock
        when(mockExtensionApi.getXDMSharedEventState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        ecidMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
        ids.add(ecidMap);
        identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ECID, ids);
        mockIdentityState.put(MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);
    }

    // ========================================================================================
    // handling rules response events
    // ========================================================================================
    @Test
    public void test_handleRulesResponseEvent_ValidConsequencePresent() {
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
        // setup
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
        // setup
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
        // setup
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
        // setup
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
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mixins = new HashMap<>();
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        Map<String, Object> cjm = new HashMap<>();
        Map<String, Object> messageProfile = new HashMap<>();
        Map<String, Object> channel = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // setup xdm map
        channel.put("_id", "https://ns.adobe.com/xdm/channels/inapp");
        messageExecution.put("messageExecutionID", "messageExecutionID");
        messageExecution.put("messagePublicationID", "messagePublicationID");
        messageExecution.put("messageID", "messageID");
        messageExecution.put("ajoCampaignVersionID", "ajoCampaignVersionID");
        messageExecution.put("ajoCampaignID", "ajoCampaignID");
        messageProfile.put("channel", channel);
        cjm.put("messageProfile", messageProfile);
        cjm.put("messageExecution", messageExecution);
        experience.put("customerJourneyManagement", cjm);
        mixins.put("_experience", experience);
        xdm.put("mixins", mixins);

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_XDM, xdm);
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
