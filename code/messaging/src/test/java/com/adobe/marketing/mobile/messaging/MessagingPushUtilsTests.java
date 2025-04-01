/*
  Copyright 2024 Adobe. All rights reserved.
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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingPushUtilsTests {

    @Mock HttpURLConnection mockConnection;

    @Mock Bitmap mockBitmap;

    @Mock Context mockContext;

    @Mock PackageManager mockPackageManager;

    @Mock ApplicationInfo mockApplicationInfo;

    @Mock Resources mockResources;

    @Mock ServiceProvider mockServiceProvider;

    @Mock CacheResult mockCacheResult;

    @Mock CacheService mockCacheService;

    @After
    public void tearDown() {
        reset(
                mockConnection,
                mockBitmap,
                mockContext,
                mockPackageManager,
                mockApplicationInfo,
                mockResources,
                mockServiceProvider,
                mockCacheService,
                mockCacheResult);
    }

    @Test
    public void downloadReturnsBitmapWhenUrlIsValid() throws Exception {
        // setup
        try (MockedStatic<BitmapFactory> bitmapFactoryMockedStatic =
                        Mockito.mockStatic(BitmapFactory.class);
                MockedConstruction<URL> urlMockedConstruction =
                        Mockito.mockConstruction(
                                URL.class,
                                (mock, context) ->
                                        when(mock.openConnection()).thenReturn(mockConnection))) {
            String validUrl = "http://valid.url";
            InputStream inputStream = new ByteArrayInputStream("".getBytes());
            when(mockConnection.getInputStream()).thenReturn(inputStream);
            when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            bitmapFactoryMockedStatic
                    .when(() -> BitmapFactory.decodeStream(inputStream))
                    .thenReturn(mockBitmap);

            // test
            Bitmap resultBitmap = MessagingPushUtils.download(validUrl);

            // verify
            assertEquals(mockBitmap, resultBitmap);
        }
    }

    @Test
    public void downloadReturnsNullWhenUrlIsInvalid() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Bitmap resultBitmap = MessagingPushUtils.download("invalid");

            // verify
            assertNull(resultBitmap);
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString()));
        }
    }

    @Test
    public void downloadReturnsNullWhenExceptionOccurs() throws Exception {
        // setup
        try (MockedStatic<BitmapFactory> bitmapFactoryMockedStatic =
                        Mockito.mockStatic(BitmapFactory.class);
                MockedConstruction<URL> urlMockedConstruction =
                        Mockito.mockConstruction(
                                URL.class,
                                (mock, context) ->
                                        when(mock.openConnection()).thenReturn(mockConnection))) {
            String validUrl = "http://valid.url";
            InputStream inputStream = new ByteArrayInputStream("".getBytes());
            doThrow(new IOException()).when(mockConnection).getInputStream();
            when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            bitmapFactoryMockedStatic
                    .when(() -> BitmapFactory.decodeStream(inputStream))
                    .thenReturn(mockBitmap);

            // test
            Bitmap resultBitmap = MessagingPushUtils.download(validUrl);

            // verify
            assertNull(resultBitmap);
        }
    }

    @Test
    public void getDefaultAppIconReturnsIconWhenPackageNameIsValid() throws Exception {
        // setup
        String validPackageName = "valid.package.name";
        int expectedIcon = 123;
        when(mockContext.getPackageName()).thenReturn(validPackageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockPackageManager.getApplicationInfo(validPackageName, 0))
                .thenReturn(mockApplicationInfo);
        mockApplicationInfo.icon = expectedIcon;

        // test
        int resultIcon = MessagingPushUtils.getDefaultAppIcon(mockContext);

        // verify
        assertEquals(expectedIcon, resultIcon);
    }

    @Test
    public void getDefaultAppIconReturnsNegativeWhenExceptionOccurs() throws Exception {
        // setup
        String validPackageName = "valid.package.name";
        when(mockContext.getPackageName()).thenReturn(validPackageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        doThrow(new PackageManager.NameNotFoundException())
                .when(mockPackageManager)
                .getApplicationInfo(validPackageName, 0);

        // test
        int resultIcon = MessagingPushUtils.getDefaultAppIcon(mockContext);

        // verify
        assertEquals(-1, resultIcon);
    }

    @Test
    public void getSoundUriForResourceNameReturnsCorrectUriWhenSoundNameIsValid() {
        // setup
        String validSoundName = "valid_sound";
        String packageName = "com.adobe.marketing.mobile.messaging";
        when(mockContext.getPackageName()).thenReturn(packageName);
        Uri expectedUri = Uri.parse("android.resource://" + packageName + "/raw/" + validSoundName);

        // test
        Uri resultUri = MessagingPushUtils.getSoundUriForResourceName(validSoundName, mockContext);

        // verify
        assertEquals(expectedUri, resultUri);
    }

    @Test
    public void getSmallIconWithResourceNameReturnsCorrectIdWhenIconNameIsValid() {
        // setup
        String validIconName = "valid_icon";
        String packageName = "com.adobe.marketing.mobile.messaging";
        int expectedIconId = 123;
        when(mockContext.getPackageName()).thenReturn(packageName);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getIdentifier(validIconName, "drawable", packageName))
                .thenReturn(expectedIconId);

        // test
        int resultIconId =
                MessagingPushUtils.getSmallIconWithResourceName(validIconName, mockContext);

        // verify
        assertEquals(expectedIconId, resultIconId);
    }

    @Test
    public void getSmallIconWithResourceNameReturnsZeroWhenIconNameIsEmpty() {
        // setup
        String emptyIconName = "";

        // test
        int resultIconId =
                MessagingPushUtils.getSmallIconWithResourceName(emptyIconName, mockContext);

        // verify
        assertEquals(0, resultIconId);
    }

    @Test
    public void getCachedRichMediaFileUriReturnsNullWhenCacheResultIsNull() {
        // test
        Uri resultUri = MessagingPushUtils.getCachedRichMediaFileUri(null);

        // verify
        assertNull(resultUri);
    }

    @Test
    public void getCachedRichMediaFileUriReturnsNullWhenCacheResultMetadataIsNull() {
        // setup
        when(mockCacheResult.getMetadata()).thenReturn(null);

        // test
        Uri resultUri = MessagingPushUtils.getCachedRichMediaFileUri(mockCacheResult);

        // verify
        assertNull(resultUri);
    }

    @Test
    public void getCachedRichMediaFileUriReturnsNullWhenPathToFileIsNull() {
        // setup
        Map<String, String> metadata = new HashMap<>();
        metadata.put("pathToFile", null);
        when(mockCacheResult.getMetadata()).thenReturn(metadata);

        // test
        Uri resultUri = MessagingPushUtils.getCachedRichMediaFileUri(mockCacheResult);

        // verify
        assertNull(resultUri);
    }

    @Test
    public void getCachedRichMediaFileUriReturnsNullWhenContextIsNull() {
        // setup
        mockServiceProvider = mock(ServiceProvider.class);
        AppContextService mockAppContextService = mock(AppContextService.class);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getApplicationContext()).thenReturn(null);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("pathToFile", "mockPathToFile");
            when(mockCacheResult.getMetadata()).thenReturn(metadata);

            // test
            Uri resultUri = MessagingPushUtils.getCachedRichMediaFileUri(mockCacheResult);

            // verify
            assertNull(resultUri);
        }
    }

    @Test
    public void getCachedRichMediaFileUriReturnsUriWhenFileExists() {
        // setup
        Map<String, String> metadata = new HashMap<>();
        metadata.put("pathToFile", "mockPathToFile");
        when(mockCacheResult.getMetadata()).thenReturn(metadata);

        mockServiceProvider = mock(ServiceProvider.class);
        AppContextService mockAppContextService = mock(AppContextService.class);
        PackageManager mockPackageManager = mock(PackageManager.class);
        Context mockContext = mock(Context.class);
        String fileName = "test_file.jpg";
        String packageName = "com.adobe.marketing.mobile.messaging";
        when(mockContext.getPackageName()).thenReturn(packageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        Uri expectedUri =
                Uri.parse("content://" + packageName + ".provider/root/messaging/" + fileName);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<FileProvider> fileProviderMockedStatic =
                        Mockito.mockStatic(FileProvider.class)) {
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getApplicationContext()).thenReturn(mockContext);

            fileProviderMockedStatic
                    .when(() -> FileProvider.getUriForFile(any(), anyString(), any()))
                    .thenReturn(expectedUri);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            // test
            Uri resultUri = MessagingPushUtils.getCachedRichMediaFileUri(mockCacheResult);

            // verify
            assertEquals(expectedUri, resultUri);
        }
    }

    @Test
    public void getCachedAssetReturnsCacheResultWhenCachedFileExists() {
        // setup
        mockCacheService = mock(CacheService.class);
        CacheResult mockCacheResult = mock(CacheResult.class);
        mockServiceProvider = mock(ServiceProvider.class);
        Executor mockExecutor = mock(Executor.class);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<CompletableFuture> completableFutureMockedStatic =
                        Mockito.mockStatic(CompletableFuture.class)) {
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            CompletableFuture<CacheResult> mockFuture = mock(CompletableFuture.class);
            when(mockFuture.join()).thenReturn(mockCacheResult);
            completableFutureMockedStatic
                    .when(
                            () ->
                                    CompletableFuture.supplyAsync(
                                            any(Supplier.class), any(Executor.class)))
                    .thenReturn(mockFuture);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            String fileName = "test_image.jpg";

            // test
            CacheResult cachedAsset =
                    MessagingPushUtils.getCachedAsset(mockExecutor, fileName, 2000).join();

            // verify
            assertEquals(mockCacheResult, cachedAsset);
        }
    }

    @Test
    public void getCachedAssetReturnsNullWhenCacheResultDoesNotExist() {
        // setup
        mockCacheService = mock(CacheService.class);
        CacheResult mockCacheResult = mock(CacheResult.class);
        mockServiceProvider = mock(ServiceProvider.class);
        Executor mockExecutor = mock(Executor.class);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<CompletableFuture> completableFutureMockedStatic =
                        Mockito.mockStatic(CompletableFuture.class)) {
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            CompletableFuture<CacheResult> mockFuture = mock(CompletableFuture.class);
            when(mockFuture.join()).thenReturn(null);
            completableFutureMockedStatic
                    .when(
                            () ->
                                    CompletableFuture.supplyAsync(
                                            any(Supplier.class), any(Executor.class)))
                    .thenReturn(mockFuture);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            String fileName = "test_image.jpg";

            // test
            CacheResult cachedAsset =
                    MessagingPushUtils.getCachedAsset(mockExecutor, fileName, 2000).join();

            // verify
            assertNull(cachedAsset);
        }
    }

    @Test
    public void tryGetCachedAssetReturnsCacheResultWhenAssetIsCached() {
        // setup
        String key = "test_key";
        int elapsedTime = 0;
        mockCacheService = mock(CacheService.class);
        CacheResult mockCacheResult = mock(CacheResult.class);
        mockServiceProvider = mock(ServiceProvider.class);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<InternalMessagingUtils> internalMessagingUtilsMockedStatic =
                        Mockito.mockStatic(InternalMessagingUtils.class)) {
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            internalMessagingUtilsMockedStatic
                    .when(InternalMessagingUtils::getAssetCacheLocation)
                    .thenReturn("assetCacheLocation");

            // test
            CacheResult result = MessagingPushUtils.tryGetCachedAsset(key, 2000);

            // verify
            assertEquals(mockCacheResult, result);
        }
    }

    @Test
    public void tryGetCachedAssetReturnsNullWhenAssetIsNotCachedWithinTimeout() {
        // setup
        String key = "test_key";
        mockCacheService = mock(CacheService.class);
        mockServiceProvider = mock(ServiceProvider.class);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<InternalMessagingUtils> internalMessagingUtilsMockedStatic =
                        Mockito.mockStatic(InternalMessagingUtils.class)) {
            when(mockCacheService.get(anyString(), anyString())).thenReturn(null);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);

            internalMessagingUtilsMockedStatic
                    .when(InternalMessagingUtils::getAssetCacheLocation)
                    .thenReturn("assetCacheLocation");

            // test
            CacheResult result = MessagingPushUtils.tryGetCachedAsset(key, 2000);

            // verify
            assertNull(result);
        }
    }
}
