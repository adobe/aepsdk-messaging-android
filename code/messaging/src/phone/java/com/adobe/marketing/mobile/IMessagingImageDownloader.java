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

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Interface defining a Messaging extension image downloader object.
 */
public interface IMessagingImageDownloader {
    /**
     * Downloads the image asset referenced in the image URL {@code String}.
     *
     * @param context  The application {@link Context}
     * @param imageUrl a {@code String} containing the image asset to be downloaded
     * @return the {@link Bitmap} created from the downloaded image asset
     */
    Bitmap getBitmapFromUrl(Context context, String imageUrl);
}
