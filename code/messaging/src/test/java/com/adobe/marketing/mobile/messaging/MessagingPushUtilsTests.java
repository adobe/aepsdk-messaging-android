package com.adobe.marketing.mobile.messaging;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.adobe.marketing.mobile.services.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(MockitoJUnitRunner.class)
public class MessagingPushUtilsTests {

    @Mock
    HttpURLConnection mockConnection;

    @Mock
    Bitmap mockBitmap;

    @Mock
    Context mockContext;

    @Mock
    PackageManager mockPackageManager;

    @Mock
    ApplicationInfo mockApplicationInfo;

    @Mock
    Resources mockResources;

    @After
    public void tearDown() {
        reset(mockConnection, mockBitmap, mockContext, mockPackageManager, mockApplicationInfo, mockResources);
    }

    @Test
    public void downloadReturnsBitmapWhenUrlIsValid() throws Exception {
        //setup
        try (MockedStatic<BitmapFactory> bitmapFactoryMockedStatic = Mockito.mockStatic(BitmapFactory.class);
             MockedConstruction<URL> urlMockedConstruction = Mockito.mockConstruction(URL.class,
                     (mock, context) -> when(mock.openConnection()).thenReturn(mockConnection))) {
            String validUrl = "http://valid.url";
            InputStream inputStream = new ByteArrayInputStream("".getBytes());
            when(mockConnection.getInputStream()).thenReturn(inputStream);
            when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            bitmapFactoryMockedStatic.when(() -> BitmapFactory.decodeStream(inputStream)).thenReturn(mockBitmap);

            //test
            Bitmap resultBitmap = MessagingPushUtils.download(validUrl);

            //verify
            assertEquals(mockBitmap, resultBitmap);
        }
    }

    @Test
    public void downloadReturnsNullWhenUrlIsInvalid() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            //test
            Bitmap resultBitmap = MessagingPushUtils.download("invalid");

            //verify
            assertNull(resultBitmap);
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), anyString(), anyString()));
        }
    }

    @Test
    public void downloadReturnsNullWhenExceptionOccurs() throws Exception {
        //setup
        try (MockedStatic<BitmapFactory> bitmapFactoryMockedStatic = Mockito.mockStatic(BitmapFactory.class);
             MockedConstruction<URL> urlMockedConstruction = Mockito.mockConstruction(URL.class,
                     (mock, context) -> when(mock.openConnection()).thenReturn(mockConnection))) {
            String validUrl = "http://valid.url";
            InputStream inputStream = new ByteArrayInputStream("".getBytes());
            doThrow(new IOException()).when(mockConnection).getInputStream();
            when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            bitmapFactoryMockedStatic.when(() -> BitmapFactory.decodeStream(inputStream)).thenReturn(mockBitmap);

            //test
            Bitmap resultBitmap = MessagingPushUtils.download(validUrl);

            //verify
            assertNull(resultBitmap);
        }
    }

    @Test
    public void getDefaultAppIconReturnsIconWhenPackageNameIsValid() throws Exception {
        //setup
        String validPackageName = "valid.package.name";
        int expectedIcon = 123;
        when(mockContext.getPackageName()).thenReturn(validPackageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockPackageManager.getApplicationInfo(validPackageName, 0)).thenReturn(mockApplicationInfo);
        mockApplicationInfo.icon = expectedIcon;

        //test
        int resultIcon = MessagingPushUtils.getDefaultAppIcon(mockContext);

        //verify
        assertEquals(expectedIcon, resultIcon);
    }

    @Test
    public void getDefaultAppIconReturnsNegativeWhenExceptionOccurs() throws Exception {
        //setup
        String validPackageName = "valid.package.name";
        when(mockContext.getPackageName()).thenReturn(validPackageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        doThrow(new PackageManager.NameNotFoundException()).when(mockPackageManager).getApplicationInfo(validPackageName, 0);

        //test
        int resultIcon = MessagingPushUtils.getDefaultAppIcon(mockContext);

        //verify
        assertEquals(-1, resultIcon);
    }

    @Test
    public void getSoundUriForResourceNameReturnsCorrectUriWhenSoundNameIsValid() {
        //setup
        String validSoundName = "valid_sound";
        String packageName = "com.adobe.marketing.mobile.messaging";
        when(mockContext.getPackageName()).thenReturn(packageName);
        Uri expectedUri = Uri.parse("android.resource://" + packageName + "/raw/" + validSoundName);

        //test
        Uri resultUri = MessagingPushUtils.getSoundUriForResourceName(validSoundName, mockContext);

        //verify
        assertEquals(expectedUri, resultUri);
    }

    @Test
    public void getSmallIconWithResourceNameReturnsCorrectIdWhenIconNameIsValid() {
        //setup
        String validIconName = "valid_icon";
        String packageName = "com.adobe.marketing.mobile.messaging";
        int expectedIconId = 123;
        when(mockContext.getPackageName()).thenReturn(packageName);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getIdentifier(validIconName, "drawable", packageName)).thenReturn(expectedIconId);

        //test
        int resultIconId = MessagingPushUtils.getSmallIconWithResourceName(validIconName, mockContext);

        //verify
        assertEquals(expectedIconId, resultIconId);
    }

    @Test
    public void getSmallIconWithResourceNameReturnsZeroWhenIconNameIsEmpty() {
        //setup
        String emptyIconName = "";

        //test
        int resultIconId = MessagingPushUtils.getSmallIconWithResourceName(emptyIconName, mockContext);

        //verify
        assertEquals(0, resultIconId);
    }
}
