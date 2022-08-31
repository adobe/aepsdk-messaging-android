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

/**
 * This class contains functionality to cache the json message payload and any image asset URL's present in an
 * AJO in-app message.
 */
final class MessagingCacheUtilities {
    private final static String SELF_TAG = "MessagingCacheUtilities";
    private final int STREAM_WRITE_BUFFER_SIZE = 4096;
    private final SystemInfoService systemInfoService;
    private final NetworkService networkService;
    private final Map<String, String> assetMap = new HashMap<>();
    private final CacheManager cacheManager;

    MessagingCacheUtilities(final SystemInfoService systemInfoService, final NetworkService networkService, final CacheManager cacheManager) throws MissingPlatformServicesException {
        if (networkService == null) {
            throw new MissingPlatformServicesException("Network service implementation missing");
        }
        if (systemInfoService == null) {
            throw new MissingPlatformServicesException("System info service implementation missing");
        }
        if (cacheManager == null) {
            throw new MissingPlatformServicesException("Cache Manager implementation missing");
        }
        this.systemInfoService = systemInfoService;
        this.networkService = networkService;
        this.cacheManager = cacheManager;
        createImageAssetsCacheDirectory();
    }
    // ========================================================================================================
    // Message payload caching
    // ========================================================================================================

    /**
     * Determines if messages have been previously cached.
     *
     * @return {@code boolean} containing true if cached messages are found, false otherwise.
     */
    boolean areMessagesCached() {
        return cacheManager.getFileForCachedURL(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, false) != null;
    }

    /**
     * Delete all contents in the {@link Messaging} extension cache subdirectory.
     *
     * @param cacheSubdirectory the {@code String} subdirectory to be cleared.
     */
    void clearCachedDataFromSubdirectory(final String cacheSubdirectory) {
        cacheManager.deleteFilesNotInList(null, cacheSubdirectory, true);
        Log.trace(LOG_TAG, "%s - In-app messaging %s cache has been deleted.", SELF_TAG, cacheSubdirectory);
    }

    /**
     * Retrieves cached {@code String} message payloads and returns them in a {@link Map<String, Variant>}.
     *
     * @return a {@code Map<String, Variant>} containing the message payloads.
     */
    Map<String, Object> getCachedMessages() {
        final File cachedMessageFile = cacheManager.getFileForCachedURL(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, false);
        if (cachedMessageFile == null) {
            Log.debug(LOG_TAG, "%s - Unable to find a cached message.", SELF_TAG);
            return null;
        }

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(cachedMessageFile);
            final String streamContents = StringUtils.streamToString(fileInputStream);
            final JSONObject cachedMessagePayload = new JSONObject(streamContents);
            return MessagingUtils.toMap(cachedMessagePayload);
        } catch (final FileNotFoundException fileNotFoundException) {
            Log.warning(LOG_TAG, "%s - Exception occurred when retrieving the cached message file: %s", SELF_TAG, fileNotFoundException.getMessage());
            return null;
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "%s - Exception occurred when creating the JSONArray: %s", SELF_TAG, jsonException.getMessage());
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, "%s - Exception occurred when closing the FileInputStream: %s", SELF_TAG, ioException.getMessage());
                return null;
            }
        }
    }

    /**
     * Caches the {@link Map <String, Object>} message payload.
     *
     * @param messagePayload the {@code Map<String, Object>} containing the message payload to be cached.
     */
    void cacheRetrievedMessages(final Map<String, Object> messagePayload) {
        // clean any existing cached files first
        clearCachedDataFromSubdirectory(MESSAGES_CACHE_SUBDIRECTORY);
        // quick out if an empty message payload was received
        if (MessagingUtils.isMapNullOrEmpty(messagePayload)) {
            return;
        }
        Log.debug(LOG_TAG, "%s - Creating new cached message definitions at: %s", SELF_TAG, cacheManager.getBaseFilePath(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY));
        final File cachedMessages = cacheManager.createNewCacheFile(CACHE_NAME, MESSAGES_CACHE_SUBDIRECTORY, new Date());
        try {
            // convert the message payload to JSON then cache the JSON as a string
            final Object json = MessagingUtils.toJSON(messagePayload);
            writeInputStreamIntoFile(cachedMessages, new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)), false);
        } catch (final JSONException e) {
            Log.error(LOG_TAG, "%s - JSONException while attempting to create JSON from ArrayList payload: (%s)", SELF_TAG, e);
        }
    }

    /**
     * Writes the provided {@link InputStream} into the provided cache {@link File}.
     * <p>
     * The content of the inputStream is appended to an existing file if the boolean is set as true.
     *
     * @param cachedFile  {@code File} to which the content has to be written
     * @param inputStream The {@code InputStream} to be written to the cache
     * @param append      true, if you want to append the input stream to the existing file content
     * @return {@code boolean} containing true if the {@code InputStream} has been successfully written into the file, false otherwise
     */
    private boolean writeInputStreamIntoFile(final File cachedFile, final InputStream inputStream, final boolean append) {
        if (cachedFile == null || inputStream == null) {
            Log.error(LOG_TAG, "%s - Failed to write InputStream to the cache. The cachedFile or inputStream is null.", SELF_TAG);
            return false;
        }

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }
            outputStream.flush();
        } catch (final IOException e) {
            Log.error(LOG_TAG, "%s - IOException while attempting to write remote file (%s)", SELF_TAG, e);
            return false;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (final IOException e) {
                Log.error(LOG_TAG, "%s - Unable to close the OutputStream (%s) ", SELF_TAG, e);
            }
        }

        return true;
    }

    // ========================================================================================================
    // Image asset caching
    // ========================================================================================================

    /**
     * Caches the assets provided in the {@link java.util.List}.
     *
     * @param assetsUrls a {@link List<String>} containing asset URL's to be cached.
     */
    void cacheImageAssets(final List<String> assetsUrls) {
        if (cacheManager == null) {
            Log.trace(LOG_TAG, "%s - Failed to cache asset, the cache manager is not available.", SELF_TAG);
            return;
        }

        final List<String> assetsToRetain = new ArrayList<>();

        // validate asset URL's and remove duplicates
        if (assetsUrls != null && !assetsUrls.isEmpty()) {
            for (final String imageAssetUrl : assetsUrls) {
                if (assetIsDownloadable(imageAssetUrl) && !assetsToRetain.contains(imageAssetUrl)) {
                    assetsToRetain.add(imageAssetUrl);
                }
            }
        }

        // clean any cached images which are no longer needed then use the RemoteDownloader to download
        // any new assets that have not been previously cached.
        cacheManager.deleteFilesNotInList(assetsToRetain, IMAGES_CACHE_SUBDIRECTORY);

        for (final String assetUrl : assetsToRetain) {
            try {
                final RemoteDownloader remoteDownloader = getRemoteDownloader(assetUrl, IMAGES_CACHE_SUBDIRECTORY);
                remoteDownloader.startDownload();
            } catch (final MissingPlatformServicesException exception) {
                Log.warning(LOG_TAG, "%s - Failed to cache asset: %s, the platform services were not available.", SELF_TAG, assetUrl);
            }
        }
    }

    /**
     * Determine whether the provided {@code String} asset URL is downloadable.
     *
     * @param assetUrl {@code String} containing the asset url to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    boolean assetIsDownloadable(final String assetUrl) {
        return StringUtils.stringIsUrl(assetUrl) && (assetUrl.startsWith("http"));
    }

    /**
     * Returns a {@link Map<String, String>} containing the remote asset mapped to it's cached location.
     *
     * @return {@code Map<String, String} containing a mapping of a remote image asset URL and it's cached location
     */
    Map<String, String> getAssetsMap() {
        return assetMap;
    }

    /**
     * Returns a {@link RemoteDownloader} object for the provided {@code String} currentAssetUrl.
     *
     * @param currentAssetUrl   {@code String} containing the URL of the asset to be downloaded
     * @param cacheSubDirectory {@code String} containing the subdirectory to store the cached asset
     * @return A {@code RemoteDownloader} configured with the {@code String} currentAssetUrl
     * @throws MissingPlatformServicesException when {@link NetworkService} or {@link SystemInfoService} are null
     */
    private RemoteDownloader getRemoteDownloader(final String currentAssetUrl, final String cacheSubDirectory) throws MissingPlatformServicesException {
        return new RemoteDownloader(networkService, systemInfoService, currentAssetUrl, cacheSubDirectory) {
            @Override
            protected void onDownloadComplete(final File downloadedFile) {
                // Update the asset map with a remote url to cached asset mapping on successful image asset download
                // which will be used by the Message Webview to load cached assets when displaying the IAM html.
                // If the download fails, use the remote url when displaying the message. Another attempt to cache the remote
                // image asset will be made on the next app launch.
                if (downloadedFile != null) {
                    Log.trace(LOG_TAG, "%s - (%s) has been downloaded or was previously cached at: (%s)", SELF_TAG, currentAssetUrl, downloadedFile.getPath());
                    assetMap.put(currentAssetUrl, downloadedFile.getAbsolutePath());
                } else {
                    Log.debug(LOG_TAG, "%s - Failed to download asset from %s.", SELF_TAG, currentAssetUrl);
                    assetMap.put(currentAssetUrl, currentAssetUrl);
                }
            }
        };
    }

    /**
     * Creates the "images" cache directory for the {@link Messaging} extension.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     */
    private void createImageAssetsCacheDirectory() {
        final File assetDir = new File(systemInfoService.getApplicationCacheDir() + File.separator + IMAGES_CACHE_SUBDIRECTORY);

        if (!assetDir.exists() && !assetDir.mkdirs()) {
            Log.warning(LOG_TAG, "%s - Unable to create directory at (%s) for caching image assets", SELF_TAG, assetDir.getAbsolutePath());
        }
    }
}
