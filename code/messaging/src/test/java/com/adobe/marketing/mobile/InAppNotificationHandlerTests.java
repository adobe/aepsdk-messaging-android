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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
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
    private static final String mockAppId = "mock_applicationId";
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
    MessagingInternal mockMessagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private MessagingCacheUtilities messagingCacheUtilities;
    private InAppNotificationHandler inAppNotificationHandler;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, InterruptedException, MissingPlatformServicesException {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupApplicationIdMocks();
        setupPlatformServicesMocks();
        setupSharedStateMocks();

        messagingCacheUtilities = new MessagingCacheUtilities(mockAndroidSystemInfoService, mockAndroidNetworkService, new CacheManager(mockAndroidSystemInfoService));
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);
    }

    void setupMocks() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        when(MobileCore.getCore()).thenReturn(mockCore);
    }

    void setupApplicationIdMocks() {
        when(App.getApplication()).thenReturn(mockApplication);
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn(mockAppId);
        when(mockContext.getPackageName()).thenReturn(mockAppId);
    }

    void setupPlatformServicesMocks() {
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);
        // setup mock cache file
        final File mockCache = new File("mock_cache");
        when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
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

    @After
    public void cleanup() {
        // use messaging cache utilities to clean the cache after each test
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY);
    }

    // ========================================================================================
    // handlePersonalizationPayload
    // ========================================================================================
    @Test
    public void test_handlePersonalizationPayload_ValidIAMPayloadPresent() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_MultipleValidIAMPayloadPresent() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 3;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify 3 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(3, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_OneInvalidIAMPayloadPresent() {
        // setup
        MessageTestConfig validPayloadConfig = new MessageTestConfig();
        validPayloadConfig.count = 2;
        MessageTestConfig invalidPayloadConfig = new MessageTestConfig();
        invalidPayloadConfig.count = 1;
        invalidPayloadConfig.isMissingRulesKey = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(validPayloadConfig);
        List<Map> invalidPayload = MessagingTestUtils.generateMessagePayload(invalidPayloadConfig);
        payload.addAll(invalidPayload);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify 2 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(2, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageId() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageId = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageType() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageType = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageDetail() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageDetail = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadIsEmpty() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.hasEmptyPayload = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadIsNull() {
        // test
        inAppNotificationHandler.handlePersonalizationPayload(null);

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    // ========================================================================================
    // handlePersonalizationPayload
    // ========================================================================================
    @Test
    public void test_handlePersonalizationPayload_PayloadContainsNonMatchingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        config.nonMatchingAppSurfaceInPayload = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_PayloadMissingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;

        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);

        // test
        inAppNotificationHandler.handlePersonalizationPayload(payload.get(0));

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    // ========================================================================================
    // inAppNotificationHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void test_cachedPropositions_ValidPayload() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cachePropositions(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(1, rules.size());
    }

    @Test
    public void test_cachedPropositions_nonMatchingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        config.nonMatchingAppSurfaceInPayload = true;
        List<Map> payload = MessagingTestUtils.generateMessagePayload(config);
        messagingCacheUtilities.cachePropositions(payload.get(0));

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }
}
