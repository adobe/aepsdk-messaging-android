/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.CACHE_NAME;
import static com.adobe.marketing.mobile.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingCacheUtilities {
    private final int STREAM_WRITE_BUFFER_SIZE = 4096;
    private final String FILE_PATH = "file://";
    private CacheManager cacheManager;
    private final SystemInfoService systemInfoService;
    private final NetworkService networkService;

    MessagingCacheUtilities(final SystemInfoService systemInfoService, final NetworkService networkService) {
        this.systemInfoService = systemInfoService;
        this.networkService = networkService;
        try {
            cacheManager = new CacheManager(systemInfoService);
        } catch (final MissingPlatformServicesException exception) {
            Log.warning(LOG_TAG, "Could not create MessagingCaching: %s", exception.getMessage());
            return;
        }
        createMessageAssetCacheDirectory();
    }

    /**
     * Determines if messages have been previously cached.
     *
     * @return {@code boolean} containing true if cached messages are found, false otherwise.
     */
    boolean areMessagesCached() {
        return cacheManager.getFileForCachedURL(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, false) != null;
    }

    /**
     * Caches the assets provided in the {@link java.util.List}.
     *
     * @param assetsCollection a {@link List<String>} containing asset url's to be cached.
     */
    void cacheImageAssets(final List<String> assetsCollection) {
        final ArrayList<String> assetsToRetain = new ArrayList<>();

        if (assetsCollection != null && !assetsCollection.isEmpty()) {
            for (final String imageAsset : assetsCollection) {
                final String url = imageAsset;
                if (assetIsDownloadable(url)) {
                    assetsToRetain.add(url);
                }
            }
        }

        // clean any existing cached images first
        cacheManager.deleteFilesNotInList(assetsToRetain, IMAGES_CACHE_SUBDIRECTORY);
        for (final String asset: assetsToRetain) {
            final String cacheDirectory = cacheManager.getFileForCachedURL(asset, IMAGES_CACHE_SUBDIRECTORY, true).getAbsolutePath();
            Log.debug(LOG_TAG, "Creating new cached image assets at: %s", cacheDirectory);
            try {
                final RemoteDownloader remoteDownloader = getRemoteDownloader(asset, cacheDirectory);
                remoteDownloader.startDownload();
            } catch (final MissingPlatformServicesException exception) {
                Log.warning(LOG_TAG, "Failed to cache asset: %s, the platform services were not available.", asset);
            }
        }
    }

    /**
     * Determine whether the provided {@code assetPath} is downloadable.
     * <p>
     * Checks that the {@code assetPath} is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param assetPath {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    boolean assetIsDownloadable(final String assetPath) {
        return StringUtils.stringIsUrl(assetPath) && (assetPath.startsWith("http") || assetPath.startsWith("https"));
    }

    /**
     * Returns a {@link RemoteDownloader} object for the provided {@code String} currentAssetUrl.
     *
     * @param currentAssetUrl {@code String} containing the URL of the asset to be downloaded
     * @param cacheSubDirectory {@code String} containing the subdirectory to store the cached asset
     * @return A {@code RemoteDownloader} configured with the {@code String} currentAssetUrl
     * @throws MissingPlatformServicesException when {@link NetworkService} or {@link SystemInfoService} are null
     */
    private RemoteDownloader getRemoteDownloader(final String currentAssetUrl, final String cacheSubDirectory) throws MissingPlatformServicesException {
        return new RemoteDownloader(networkService, systemInfoService, currentAssetUrl, cacheSubDirectory) {
            @Override
            protected void onDownloadComplete(final File downloadedFile) {
                if (downloadedFile != null) {
                    Log.trace(LOG_TAG, "%s has been downloaded and cached.", currentAssetUrl);
                } else {
                    Log.debug(LOG_TAG, "Failed to download asset from %s.", currentAssetUrl);
                }
            }
        };
    }

    /**
     * Creates the "images" cache directory for the {@link Messaging} extension.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     */
    private void createMessageAssetCacheDirectory() {
        try {
            final File assetDir = new File(systemInfoService.getApplicationCacheDir() + File.separator
                    + IMAGES_CACHE_SUBDIRECTORY);

            if (!assetDir.exists() && !assetDir.mkdirs()) {
                Log.warning(LOG_TAG,
                        "Unable to create directory for caching message assets");
            }
        } catch (final Exception ex) {
            Log.warning(LOG_TAG, "An unexpected error occurred while managing assets cache directory: \n %s", ex);
        }
    }

    /**
     * Caches the {@link Map <String, Variant>} message payload.
     *
     * @param messagePayload the {@code Map<String, Variant>} containing the message payload to be cached.
     */
    void cacheRetrievedMessages(final Map<String, Variant> messagePayload) {
        // clean any existing cached files first
        clearCachedDataFromSubdirectory(MESSAGES_CACHE_SUBDIRECTORY);
        // quick out if an empty message payload was received
        if (messagePayload == null || messagePayload.isEmpty()) {
            return;
        }
        Log.debug(LOG_TAG, "Creating new cached message definitions at: %s", cacheManager.getBaseFilePath(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY));
        final Date date = new Date(System.currentTimeMillis());
        final File cachedMessages = cacheManager.createNewCacheFile(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, date);
        try {
            // convert the message payload to JSON then cache the JSON as a string
            final Object json = toJSON(messagePayload);
            readInputStreamIntoFile(cachedMessages, new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)), false);
        } catch (final JSONException e) {
            Log.error(LOG_TAG, "JSONException while attempting to create JSON from ArrayList payload: (%s)", e);
        }
    }

    /**
     * Delete all contents in the {@link Messaging} extension cache subdirectory.
     *
     * @param cacheSubdirectory the {@code String} subdirectory to be cleared.
     */
    void clearCachedDataFromSubdirectory(final String cacheSubdirectory) {
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), cacheSubdirectory, true);
        Log.trace(LOG_TAG, "In-app messaging %s cache has been deleted.", cacheSubdirectory);
    }

    /**
     * Retrieves cached {@code String} message payloads and returns the messages in a {@link Map<String, Variant>}.
     *
     * @return a {@code Map<String, Variant>} containing the message payloads.
     */
    Map<String, Variant> getCachedMessages() {
        final File cachedMessageFile = cacheManager.getFileForCachedURL(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, false);
        if (cachedMessageFile == null) {
            Log.trace(LOG_TAG, "Unable to find a cached message.");
            return null;
        }

        try {
            final FileInputStream fileInputStream = new FileInputStream(cachedMessageFile);
            final String streamContents = StringUtils.streamToString(fileInputStream);
            fileInputStream.close();
            final JSONObject cachedMessagePayload = new JSONObject(streamContents);
            return MessagingUtils.toVariantMap(cachedMessagePayload);
        } catch (final FileNotFoundException fileNotFoundException) {
            Log.warning(LOG_TAG, "Exception occurred when retrieving the cached message file: %s", fileNotFoundException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(LOG_TAG, "Exception occurred when converting the cached message file to a string: %s", ioException.getMessage());
            return null;
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "Exception occurred when creating the JSONArray: %s", jsonException.getMessage());
            return null;
        }
    }

    String createCachedImageAssetUrl(final String remoteUrl) {
        final File cachedMessageFile = cacheManager.createNewCacheFile(remoteUrl, IMAGES_CACHE_SUBDIRECTORY, new Date(System.currentTimeMillis()));
        if (cachedMessageFile == null) {
            Log.trace(LOG_TAG, "Unable to create a cached file for the url: %s", remoteUrl);
            return null;
        }

        return cacheManager.getFileForCachedURL(remoteUrl, IMAGES_CACHE_SUBDIRECTORY, true).getAbsolutePath();
    }

    /**
     * Converts provided {@link Object} to a {@link JSONObject} or {@link JSONArray}.
     *
     * @param object to be converted to jSON
     * @return {@link Object} containing a json object or json array
     */
    protected Object toJSON(final Object object) throws JSONException {
        if (object instanceof HashMap) {
            JSONObject jsonObject = new JSONObject();
            final HashMap map = (HashMap) object;
            for (final Object key : map.keySet()) {
                jsonObject.put(key.toString(), toJSON(map.get(key)));
            }
            return jsonObject;
        } else if (object instanceof Iterable) {
            JSONArray jsonArray = new JSONArray();
            for (final Object value : ((Iterable) object)) {
                jsonArray.put(toJSON(value));
            }
            return jsonArray;
        } else {
            return object;
        }
    }

    /**
     * Writes the inputStream into the file.
     * <p>
     * The content of the inputStream is appended to an existing file if the boolean is set as true.
     *
     * @param cachedFile File to which the content has to be written
     * @param input      The inputstream to be written to the cache
     * @param append     true, if you want to append the input stream to the existing file content
     * @return {@code boolean} containing true if the inputstream has been successfully written into the file, false otherwise
     */
    private boolean readInputStreamIntoFile(final File cachedFile, final InputStream input, final boolean append) {
        boolean result;

        if (cachedFile == null || input == null) {
            return false;
        }

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            result = true;
        } catch (final IOException e) {
            Log.error(LOG_TAG, "IOException while attempting to write remote file (%s)",
                    e);
            return false;
        } catch (final Exception e) {
            Log.error(LOG_TAG, "Unexpected exception while attempting to write remote file (%s)",
                    e);
            return false;
        } finally {
            try {
                if (output != null) {
                    output.close();
                }

            } catch (final Exception e) {
                Log.error(LOG_TAG, "Unable to close the OutputStream (%s) ", e);
            }
        }

        return result;
    }

    // for unit tests
    protected void setCacheManager(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
