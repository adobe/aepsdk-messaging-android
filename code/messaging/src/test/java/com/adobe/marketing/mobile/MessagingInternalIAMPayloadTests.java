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

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalIAMPayloadTests {

    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private Map<String, Object> mockConfigState = new HashMap<>();
    private Map<String, Object> mockIdentityState = new HashMap<>();

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    MessagingState messagingState;
    @Mock
    Application mockApplication;
    @Mock
    Context context;
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    UIService mockUIService;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;

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

        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, any(Event.class), nullable(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.ORG_ID, "mock_org_id");

        // setup identity shared state mock
        when(mockExtensionApi.getSharedEventState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, any(Event.class), nullable(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        mockIdentityState.put(MessagingConstants.SharedState.Configuration.ORG_ID, "mock_org_id");

        messagingInternal = new MessagingInternal(mockExtensionApi);
    }

    // ========================================================================================
    // IAM rules retrieval from Offers
    // ========================================================================================
    @Test
    public void test_fetchMessages_CalledOnExtensionStart() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // expected dispatched event data
        String expectedOffersEventData = "{\"decisionscopes\":[{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}],\"type\":\"prefetch\"}";

        // verify dispatch event is called
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
    }

    @Test
    public void test_refreshInAppMessages_Invoked() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        String expectedOffersEventData = "{\"decisionscopes\":[{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}],\"type\":\"prefetch\"}";

        // Mocks
        Event mockEvent = mock(Event.class);
        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        // when get eventData called return data with "REFRESH_MESSAGES, true"
        when(mockEvent.getEventData()).thenReturn(eventData.toObjectMap());

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify dispatch event is called
        // 2 events dispatched: Offers iam fetch event when extension is registered + Offers iam fetch event when refresh in app messages event is received
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify events
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
        event = eventCaptor.getAllValues().get(1);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
    }

    @Test
    public void test_refreshInAppMessages_Invoked_WhenPackageManagerManifestRetrievalFails() {
        // setup package manager to throw NameNotFoundException
        try {
            when(packageManager.getApplicationInfo(anyString(), anyInt())).thenThrow(PackageManager.NameNotFoundException.class);
            messagingInternal = new MessagingInternal(mockExtensionApi);
        } catch (PackageManager.NameNotFoundException e) { }
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        String expectedOffersEventData = "{\"decisionscopes\":[{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}],\"type\":\"prefetch\"}";

        // Mocks
        Event mockEvent = mock(Event.class);
        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        // when get eventData called return data with "REFRESH_MESSAGES, true"
        when(mockEvent.getEventData()).thenReturn(eventData.toObjectMap());

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 1 event dispatched (Offers iam fetch event when extension is registered)
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify events
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
    }

    // ========================================================================================
    // Offers rules payload processing
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_ValidOffersIAMPayloadPresent() throws Exception {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payload
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_MultipleValidOffersIAMPayloadPresent() throws Exception {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage2\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me2<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payloads
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        String secondExpectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me2</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing multiple valid offers iam payloads
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        assertEquals(2, entry.getValue().size());
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
        entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(secondExpectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_OneInvalidIAMPayloadPresent() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage2\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me2<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payloads
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me2</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing one valid and one invalid offers iam payloads
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        assertEquals(1, entry.getValue().size());
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageType() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageDetail() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadJsonIsEmpty() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("{}");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadJsonIsNull() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", null);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadIsEmpty() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidActivityId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"invalid\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidPlacementId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_org_id\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"invalid\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }
}
