/*
  Copyright 2022 Adobe. All rights reserved.
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class InAppNotificationHandlerTests {
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    private final String testActivityAndPlacement = "{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}";
    private final String testApplicationId = "{\"xdm:name\":\"mock_applicationId\"}";
    private final String nonMatchingApplicationId = "{\"xdm:name\":\"non_matching_applicationId\"}";
    private final String base64EncodedActivityAndPlacement = "eyJhY3Rpdml0eUlkIjoibW9ja19hY3Rpdml0eSIsInBsYWNlbWVudElkIjoibW9ja19wbGFjZW1lbnQiLCJpdGVtQ291bnQiOjMwfQ==";
    private final String base64EncodedApplicationId = "eyJ4ZG06bmFtZSI6Im1vY2tfYXBwbGljYXRpb25JZCJ9";
    private final String base64EncodedOtherApplicationId = "eyJ4ZG06bmFtZSI6Im5vbl9tYXRjaGluZ19hcHBsaWNhdGlvbklkIn0=";
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private MessagingCacheUtilities messagingCacheUtilities;
    private InAppNotificationHandler inAppNotificationHandler;

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
    UIService mockUIService;
    @Mock
    AndroidEncodingService mockAndroidEncodingService;
    @Mock
    AndroidSystemInfoService mockAndroidSystemInfoService;
    @Mock
    AndroidNetworkService mockAndroidNetworkService;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;
    @Mock
    MessagingInternal mockMessagingInternal;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, InterruptedException, MissingPlatformServicesException {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        when(MobileCore.getCore()).thenReturn(mockCore);

        // setup activity id mocks
        when(App.getApplication()).thenReturn(mockApplication);
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn("mock_applicationId");
        when(mockContext.getPackageName()).thenReturn("mock_applicationId");
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "metaData", bundle);
        when(bundle.getString("activityId")).thenReturn("mock_activity");
        when(bundle.getString("placementId")).thenReturn("mock_placement");

        // setup services mocks
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);
        // mock for encoded/decoded activity and placement id
        when(mockAndroidEncodingService.base64Encode(testActivityAndPlacement.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedActivityAndPlacement.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedActivityAndPlacement)).thenReturn(testActivityAndPlacement.getBytes(StandardCharsets.UTF_8));
        // mock for encoded/decoded application id
        when(mockAndroidEncodingService.base64Encode(testApplicationId.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedApplicationId.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedApplicationId)).thenReturn(testApplicationId.getBytes(StandardCharsets.UTF_8));
        // mock for encoded/decoded non matching application id
        when(mockAndroidEncodingService.base64Encode(nonMatchingApplicationId.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedOtherApplicationId.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedOtherApplicationId)).thenReturn(nonMatchingApplicationId.getBytes(StandardCharsets.UTF_8));

        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "mock_dataset_id");

        // setup identity shared state mock
        when(mockExtensionApi.getXDMSharedEventState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        ecidMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
        ids.add(ecidMap);
        identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ECID, ids);
        mockIdentityState.put(MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        // setup mock cache file
        final File mockCache = new File("mock_cache");
        when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);

        messagingCacheUtilities = new MessagingCacheUtilities(mockAndroidSystemInfoService, mockAndroidNetworkService);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);
    }

    @After
    public void cleanup() {
        // use messaging cache utilities to clean the cache after each test
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY);
    }

    // ========================================================================================
    // handleOfferNotificationPayload, activity id and placement id present
    // ========================================================================================
    @Test
    public void test_handleOfferNotificationPayload_ValidOffersIAMPayloadPresent() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_MultipleValidOffersIAMPayloadPresent() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 3;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
      
        // test
       inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify 3 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(3, loadedRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OneInvalidIAMPayloadPresent() {
        // setup
        MessagePayloadConfig validPayloadConfig = new MessagePayloadConfig();
        validPayloadConfig.count = 2;
        MessagePayloadConfig invalidPayloadConfig = new MessagePayloadConfig();
        invalidPayloadConfig.count = 1;
        invalidPayloadConfig.isMissingRulesKey = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(validPayloadConfig);
        List<Map> invalidPayload = MessagingTestUtils.generateMessagePayload(invalidPayloadConfig);
        payload.addAll(invalidPayload);
        
        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify 2 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(2, loadedRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadMissingMessageId() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isMissingMessageId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadMissingMessageType() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isMissingMessageType = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadMissingMessageDetail() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isMissingMessageDetail = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadIsEmpty() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.hasEmptyPayload = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadIsNull() {
        // test
        inAppNotificationHandler.handleOfferNotificationPayload(null);

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadHasInvalidActivityId() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.invalidActivityId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersIAMPayloadHasInvalidPlacementId() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.invalidPlacementId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    // ========================================================================================
    // handleOfferNotificationPayload, application id present
    // ========================================================================================
    @Test
    public void test_handleOfferNotificationPayload_OffersConfigUsingApplicationId_ValidOffersIAMPayloadPresent() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersConfigUsingApplicationId_PayloadContainsNonMatchingApplicationId() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        config.invalidApplicationId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    @Test
    public void test_handleOfferNotificationPayload_OffersConfigUsingApplicationId_PayloadMissingScope() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        config.hasNoScopeInPayload = true;

        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0));

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    // ========================================================================================
    // inAppNotificationHandler load cached messages on instantiation
    // ========================================================================================
    @Test
    public void test_cachedMessagePayload_OffersConfigUsingActivityAndPlacement_ValidPayload() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cacheRetrievedMessages(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(1, rules.size());
    }

    @Test
    public void test_cachedMessagePayload_OffersConfigUsingActivityAndPlacement_nonMatchingActivityId() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.invalidActivityId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cacheRetrievedMessages(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    @Test
    public void test_cachedMessagePayload_OffersConfigUsingActivityAndPlacement_nonMatchingPlacementId() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.invalidPlacementId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cacheRetrievedMessages(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    @Test
    public void test_cachedMessagePayload_OffersConfigUsingApplicationId_ValidPayload() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);

        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cacheRetrievedMessages(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(1, rules.size());
    }

    @Test
    public void test_cachedMessagePayload_OffersConfigUsingApplicationId_nonMatchingApplicationId() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);

        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        config.invalidApplicationId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cacheRetrievedMessages(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }
}
