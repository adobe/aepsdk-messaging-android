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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/** An entity uniquely defined by a URI that can be interacted with. */
public class Surface implements Serializable {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "Surface";
    private static final String SURFACE_BASE = "mobileapp://";
    private static final String UNKNOWN_SURFACE = "unknown";
    private static final String URI_KEY = "uri";
    private String uri;

    public Surface(final String path) {
        this(false, path);
    }

    public Surface() {
        this(
                true,
                !StringUtils.isNullOrEmpty(
                                ServiceProvider.getInstance()
                                        .getDeviceInfoService()
                                        .getApplicationPackageName())
                        ? SURFACE_BASE
                                + ServiceProvider.getInstance()
                                        .getDeviceInfoService()
                                        .getApplicationPackageName()
                        : null);
    }

    private Surface(final boolean isFullPathString, final String path) {
        if (!isFullPathString) {
            final String packageName =
                    ServiceProvider.getInstance()
                            .getDeviceInfoService()
                            .getApplicationPackageName();
            this.uri =
                    StringUtils.isNullOrEmpty(packageName)
                            ? UNKNOWN_SURFACE
                            : StringUtils.isNullOrEmpty(path)
                                    ? SURFACE_BASE + packageName
                                    : SURFACE_BASE + packageName + File.separator + path;
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

    public int hashCode() {
        return uri.hashCode();
    }

    public boolean equals(final Object object) {
        if (object instanceof Surface) {
            final Surface surface = (Surface) object;
            return (surface.getUri().equals(this.uri));
        } else {
            return false;
        }
    }

    static Surface fromUriString(final String uri) {
        final Surface surface = new Surface(true, uri);
        return !surface.isValid() ? null : surface;
    }

    public Map<String, Object> toEventData() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(URI_KEY, this.uri);
        return eventData;
    }

    public static Surface fromEventData(final Map<String, Object> data) {
        if (MapUtils.isNullOrEmpty(data) || !data.containsKey(URI_KEY)) {
            Log.debug(
                    LOG_TAG,
                    SELF_TAG,
                    "Cannot create Surface object, provided data Map is empty or null.");
            return null;
        }

        final String uri = DataReader.optString(data, URI_KEY, null);
        if (StringUtils.isNullOrEmpty(uri)) {
            Log.debug(
                    LOG_TAG,
                    SELF_TAG,
                    "Cannot create Surface object, provided data does not contain a valid uri.");
            return null;
        }

        return Surface.fromUriString(uri);
    }

    private void readObject(final ObjectInputStream objectInputStream)
            throws ClassNotFoundException, IOException {
        uri = objectInputStream.readUTF();
    }

    private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(uri);
    }
}
