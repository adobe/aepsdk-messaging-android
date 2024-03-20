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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SurfaceTests {

    @Mock ServiceProvider mockServiceProvider;
    @Mock DeviceInforming mockDeviceInfoService;

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            runnable.run();
        }
    }

    @Test
    public void test_createSurfaceWithPath() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface = new Surface("surfacewithfeeds");
                    // verify
                    assertNotNull(surface);
                    assertEquals("mobileapp://mockPackageName/surfacewithfeeds", surface.getUri());
                    assertTrue(surface.isValid());
                });
    }

    @Test
    public void test_createSurfaceWithEmptyPath() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface = new Surface("");
                    // verify
                    assertNotNull(surface);
                    assertEquals("mobileapp://mockPackageName", surface.getUri());
                    assertTrue(surface.isValid());
                });
    }

    @Test
    public void test_createSurfaceWithNullPath() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface = new Surface(null);
                    // verify
                    assertNotNull(surface);
                    assertEquals("mobileapp://mockPackageName", surface.getUri());
                    assertTrue(surface.isValid());
                });
    }

    @Test
    public void test_createSurfaceWithNoArguments() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface = new Surface();
                    // verify
                    assertNotNull(surface);
                    assertEquals("mobileapp://mockPackageName", surface.getUri());
                    assertTrue(surface.isValid());
                });
    }

    @Test
    public void test_createValidSurfaceWithPackageNameUnavailable() {
        runUsingMockedServiceProvider(
                () -> {
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
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface invalidSurface = new Surface("##invalid##");
                    // verify
                    assertNotNull(invalidSurface);
                    assertEquals(
                            "mobileapp://mockPackageName/##invalid##", invalidSurface.getUri());
                    assertFalse(invalidSurface.isValid());
                });
    }

    @Test
    public void test_fromStringValidString() {
        // test
        Surface surface = Surface.fromUriString("mobileapp://mockPackageName/validpath");
        // verify
        assertNotNull(surface);
        assertEquals("mobileapp://mockPackageName/validpath", surface.getUri());
        assertTrue(surface.isValid());
    }

    @Test
    public void test_fromStringInvalidString() {
        // test
        Surface surface = Surface.fromUriString("invalidstring");
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromStringWhenStringIsEmpty() {
        // test
        Surface surface = Surface.fromUriString("");
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromStringWhenStringIsNull() {
        // test
        Surface surface = Surface.fromUriString(null);
        // verify
        assertNull(surface);
    }

    @Test
    public void test_toEventData() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Map<String, Object> eventData = new Surface().toEventData();

                    // verify
                    Assert.assertEquals("mobileapp://mockPackageName", eventData.get("uri"));
                });
    }

    @Test
    public void test_fromEventData_validData() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("uri", "mobileapp://surface/path");
        // test
        Surface surface = Surface.fromEventData(eventData);
        // verify
        assertEquals("mobileapp://surface/path", surface.getUri());
    }

    @Test
    public void test_fromEventData_nullEventKey() {
        // test
        Surface surface = Surface.fromEventData(null);
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromEventData_emptyEventKey() {
        // test
        Surface surface = Surface.fromEventData(new HashMap<>());
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromEventData_invalidUri() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("uri", "path");
        // test
        Surface surface = Surface.fromEventData(eventData);
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromEventData_nullUri() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("uri", null);
        // test
        Surface surface = Surface.fromEventData(eventData);
        // verify
        assertNull(surface);
    }

    @Test
    public void test_fromEventData_missingUriKey() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("notAUriKey", "mobileapp://surface/path");
        // test
        Surface surface = Surface.fromEventData(eventData);
        // verify
        assertNull(surface);
    }

    @Test
    public void test_equals() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface1 = new Surface("surfacewithfeeds");
                    Surface surface2 = new Surface("surfacewithfeeds");
                    Surface surface3 = new Surface("othersurfacewithfeeds");
                    Object notASurface = new Object();
                    // verify
                    assertEquals(surface1, surface2);
                    assertNotEquals(surface1, surface3);
                    assertNotEquals(surface1, notASurface);
                });
    }

    @Test
    public void test_equals_ConstructorWithoutPaths() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface1 = new Surface();
                    Surface surface2 = new Surface();

                    // verify
                    assertEquals(surface1, surface2);
                    assertTrue(surface1.equals(surface2));
                });
    }

    @Test
    public void test_equals_DifferentSurfaces() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Surface surface1 = new Surface();
                    Surface surface2 = new Surface("surfacewithfeeds");

                    // verify
                    assertNotEquals(surface1, surface2);
                    assertFalse(surface1.equals(surface2));
                });
    }
}
