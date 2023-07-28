/*
  Copyright 2023 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SurfaceTests {

    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    DeviceInforming mockDeviceInfoService;

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("com.app.appname");

            runnable.run();
        }
    }

    @Test
    public void test_createValidSurfaceWithPath() {
        runUsingMockedServiceProvider(() -> {
            // test
            Surface surface = new Surface("surfacewithfeeds");
            // verify
            assertNotNull(surface);
            assertEquals("mobileapp://com.app.appname/surfacewithfeeds", surface.getUri());
            assertTrue(surface.isValid());
        });
    }

    @Test
    public void test_createValidSurfaceWithEmptyPath() {
        runUsingMockedServiceProvider(() -> {
            // test
            Surface surface = new Surface("");
            // verify
            assertNotNull(surface);
            assertEquals("mobileapp://com.app.appname", surface.getUri());
            assertTrue(surface.isValid());
        });
    }

    @Test
    public void test_createValidSurfaceWithNullPath() {
        runUsingMockedServiceProvider(() -> {
            // test
            Surface surface = new Surface(null);
            // verify
            assertNotNull(surface);
            assertEquals("mobileapp://com.app.appname", surface.getUri());
            assertTrue(surface.isValid());
        });
    }

    @Test
    public void test_createValidSurfaceWithNoArguments() {
        runUsingMockedServiceProvider(() -> {
            // test
            Surface surface = new Surface();
            // verify
            assertNotNull(surface);
            assertEquals("mobileapp://com.app.appname", surface.getUri());
            assertTrue(surface.isValid());
        });
    }

    @Test
    public void test_createValidSurfaceWithPackageNameUnavailable() {
        runUsingMockedServiceProvider(() -> {
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn(null);
            // test
            Surface surface = new Surface("validsurface");
            // verify
            assertNotNull(surface);
            assertEquals("unknown", surface.getUri());
            assertFalse(surface.isValid());
        });
    }

    @Test
    public void test_createInvalidSurface() {
        runUsingMockedServiceProvider(() -> {
            // test
            Surface invalidSurface = new Surface("##invalid##");
            // verify
            assertNotNull(invalidSurface);
            assertEquals("mobileapp://com.app.appname/##invalid##", invalidSurface.getUri());
            assertFalse(invalidSurface.isValid());
        });
    }
}
