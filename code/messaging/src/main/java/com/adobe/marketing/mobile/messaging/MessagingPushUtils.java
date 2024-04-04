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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for Building AJO push notification.
 *
 * <p>This class is used internally by the Messaging extension's push builder to build the push
 * notification. This class is not intended to be used by the customers.
 */
class MessagingPushUtils {
    private static final String SELF_TAG = "MessagingPushUtils";

    static Bitmap download(final String url) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            final URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            inputStream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to download push notification image from url (%s). Exception: %s",
                    url,
                    e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.warning(
                            MessagingPushConstants.LOG_TAG,
                            SELF_TAG,
                            "IOException during closing Input stream while push notification image"
                                    + " from url (%s). Exception: %s ",
                            url,
                            e.getMessage());
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }

    static int getDefaultAppIcon(final Context context) {
        final String packageName = context.getPackageName();
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Package manager NameNotFoundException while reading default application icon."
                            + " Exception: %s",
                    e.getMessage());
        }
        return -1;
    }

    /**
     * Returns the Uri for the sound file with the given name. The sound file must be in the res/raw
     * directory. The sound file should be in format of .mp3, .wav, or .ogg
     *
     * @param soundName the name of the sound file
     * @param context the application {@link Context}
     * @return the Uri for the sound file with the given name
     */
    static Uri getSoundUriForResourceName(final @NonNull String soundName, final Context context) {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + context.getPackageName()
                        + "/raw/"
                        + soundName);
    }

    /**
     * Returns the resource id for the icon with the given name. The icon file must be in the
     * res/drawable directory. If the icon file is not found, 0 is returned.
     *
     * @param iconName the name of the icon file
     * @param context the application {@link Context}
     * @return the resource id for the icon with the given name
     */
    static int getSmallIconWithResourceName(final String iconName, final Context context) {
        if (StringUtils.isNullOrEmpty(iconName)) {
            return 0;
        }
        return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    }
}
