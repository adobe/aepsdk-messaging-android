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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BitmapFactory.class, MessagingImageDownloaderTask.class})
public class MessagingImageDownloaderTaskTests {
    MessagingImageDownloaderTask messagingImageDownloaderTask;

    @Before
    public void before() {
        PowerMockito.mockStatic(BitmapFactory.class);
    }

    @Test
    public void test_download() throws Exception{
        // setup
        String imageUrl = "https://www.adobe.com/image.jpg";

        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
        HttpURLConnection mockUrlConnection = Mockito.mock(HttpURLConnection.class);
        URL mockURL = PowerMockito.mock(URL.class);
        InputStream mockInputStream = Mockito.mock(InputStream.class);

        PowerMockito.when(mockURL.openConnection()).thenReturn(mockUrlConnection);
        PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(mockURL);
        PowerMockito.when(mockUrlConnection.getInputStream()).thenReturn(mockInputStream);
        PowerMockito.when(BitmapFactory.decodeStream(any(InputStream.class))).thenReturn(mockBitmap);

        messagingImageDownloaderTask = new MessagingImageDownloaderTask(imageUrl);

        // test
        Bitmap bitmap = messagingImageDownloaderTask.call();
        //verify
        assertEquals(mockBitmap, bitmap);
    }
}
