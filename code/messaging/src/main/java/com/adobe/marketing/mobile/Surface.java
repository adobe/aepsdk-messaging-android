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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An entity uniquely defined by a URI that can be interacted with.
 */
public class Surface {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "Surface";
    private static final String SURFACE_BASE = "mobileapp://";
    private static final String UNKNOWN_SURFACE = "unknown";
    private final String uri;

    public Surface(final String path) {
        this(false, path);
    }

    public Surface() {
        this(false, null);
    }

    private Surface(final boolean isFullPathString, final String path) {
        if (!isFullPathString) {
            final String packageName = ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName();
            final String baseUri = StringUtils.isNullOrEmpty(packageName) ? UNKNOWN_SURFACE : SURFACE_BASE + packageName;
            this.uri = StringUtils.isNullOrEmpty(path) || baseUri.equals(UNKNOWN_SURFACE) ? baseUri : baseUri + File.separator + path;
        } else {
            this.uri = StringUtils.isNullOrEmpty(path) ? UNKNOWN_SURFACE : path;
        }
    }

    public String getUri() {
        return uri;
    }

    public boolean isValid() {
        try {
            new URI(this.uri);
        } catch (final URISyntaxException uriSyntaxException) {
            Log.warning(LOG_TAG, SELF_TAG, "Invalid surface URI found: %s", this.uri);
            return false;
        }

        return this.uri.startsWith(SURFACE_BASE);
    }

    public static Surface fromString(final String path) {
        final Surface surface = new Surface(true, path);
        return !surface.isValid() ? null : surface;
    }
}