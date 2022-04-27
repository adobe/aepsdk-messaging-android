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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.adobe.marketing.mobile.messaging.BuildConfig;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({MobileCore.class, ExtensionApi.class, App.class, Context.class, Event.class})
@RunWith(PowerMockRunner.class)
public class OffersConfigTests {
    private EventHub eventHub;
    private OffersConfig config;

    @Mock
    Application mockApplication;
    @Mock
    Context mockContext;
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;

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
        when(bundle.getString("activityId")).thenReturn("some_activityId");
        when(bundle.getString("placementId")).thenReturn("some_placementId");
    }

    @After
    public void reset() {
        BuildConfigSetter.resetFlags();
    }

    @Test
    public void test_useE2ETestConfig() {
        // setup
        BuildConfigSetter.setE2ETestConfig();

        // test
        config = new OffersConfig();

        // verify
        Assert.assertEquals("xcore:offer-placement:142ae10d1d2fd883", config.placementId);
        Assert.assertEquals("xcore:offer-activity:14b556c11d4c2433", config.activityId);
        Assert.assertNull(config.applicationId);
    }

    @Test
    public void test_useFunctionalTestConfig() {
        // setup
        BuildConfigSetter.setFunctionalTestConfig();

        // test
        config = new OffersConfig();

        // verify
        Assert.assertEquals("mock_placement", config.placementId);
        Assert.assertEquals("mock_activity", config.activityId);
        Assert.assertNull(config.applicationId);
    }

    @Test
    public void test_useActivityAndPlacementConfig_IfBothValuesArePresent() {
        // test
        config = new OffersConfig();

        // verify
        Assert.assertEquals("some_placementId", config.placementId);
        Assert.assertEquals("some_activityId", config.activityId);
        Assert.assertNull(config.applicationId);
    }

    @Test
    public void test_useApplicationIdConfig_WhenNoActivity() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        // test
        config = new OffersConfig();

        // verify
        Assert.assertNull(config.placementId);
        Assert.assertNull(config.activityId);
        Assert.assertEquals("mock_applicationId", config.applicationId);
    }

    @Test
    public void test_useApplicationIdConfig_WhenNoPlacement() {
        // setup
        when(bundle.getString("placementId")).thenReturn(null);
        // test
        config = new OffersConfig();

        // verify
        Assert.assertNull(config.placementId);
        Assert.assertNull(config.activityId);
        Assert.assertEquals("mock_applicationId", config.applicationId);
    }
}

class BuildConfigSetter {
    public static void setE2ETestConfig() {
        BuildConfig.IS_E2E_TEST.set(true);
    }

    public static void setFunctionalTestConfig() {
        BuildConfig.IS_FUNCTIONAL_TEST.set(true);
    }

    public static void resetFlags() {
        BuildConfig.IS_E2E_TEST.set(false);
        BuildConfig.IS_FUNCTIONAL_TEST.set(false);
    }
}