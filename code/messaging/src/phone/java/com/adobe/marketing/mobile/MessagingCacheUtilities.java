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
import static com.adobe.marketing.mobile.MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains functionality to cache the json message payload and any image asset URL's present in an
 * AJO in-app message definition.
 */
final class MessagingCacheUtilities {
    private final static String SELF_TAG = "MessagingCacheUtilities";
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
     * Determines if propositions have been previously cached.
     *
     * @return {@code boolean} containing true if cached propositions are found, false otherwise.
     */
    boolean arePropositionsCached() {
        return cacheManager.getFileForCachedURL(CACHE_NAME, PROPOSITIONS_CACHE_SUBDIRECTORY, false) != null;
    }

    /**
     * Delete all contents in the {@link Messaging} extension cache subdirectory.
     */
    void clearCachedData() {
        cacheManager.deleteFilesNotInList(null, PROPOSITIONS_CACHE_SUBDIRECTORY, true);
        cacheManager.deleteFilesNotInList(null, IMAGES_CACHE_SUBDIRECTORY, true);
        Log.trace(LOG_TAG, "%s - In-app messaging %s and %s caches have been deleted.", SELF_TAG, PROPOSITIONS_CACHE_SUBDIRECTORY, IMAGES_CACHE_SUBDIRECTORY);
    }

    /**
     * Retrieves cached {@code String} proposition payloads and returns them in a {@link List<PropositionPayload>}.
     *
     * @return a {@code List<PropositionPayload>} containing the cached proposition payloads.
     */
    List<PropositionPayload> getCachedPropositions() {
        final File cachedMessageFile = cacheManager.getFileForCachedURL(CACHE_NAME, PROPOSITIONS_CACHE_SUBDIRECTORY, false);
        if (cachedMessageFile == null) {
            Log.trace(LOG_TAG, "%s - Unable to find a cached proposition.", SELF_TAG);
            return null;
        }

        Log.trace(LOG_TAG, "%s - Loading cached proposition from (%s)", SELF_TAG, cachedMessageFile.getPath());
        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        List<PropositionPayload> cachedPropositions;
        try {
            fileInputStream = new FileInputStream(cachedMessageFile);
            objectInputStream = new ObjectInputStream(fileInputStream);
            cachedPropositions = (List<PropositionPayload>) objectInputStream.readObject();
        } catch (final FileNotFoundException fileNotFoundException) {
            Log.warning(LOG_TAG, "%s - Exception occurred when retrieving the cached proposition file: %s", SELF_TAG, fileNotFoundException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(LOG_TAG, "%s - Exception occurred when reading from the cached file: %s", SELF_TAG, ioException.getMessage());
            return null;
        } catch (final ClassNotFoundException classNotFoundException) {
            Log.warning(LOG_TAG, "%s - Class not found: %s", SELF_TAG, classNotFoundException.getMessage());
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, "%s - Exception occurred when closing the FileInputStream: %s", SELF_TAG, ioException.getMessage());
                return null;
            }
        }
        return cachedPropositions;
    }

    /**
     * Caches the {@code List<PropositionPayload>} payload.
     *
     * @param propositionPayload the {@link List<PropositionPayload>} containing the message payload to be cached.
     */
    void cachePropositions(final List<PropositionPayload> propositionPayload) {
        // clean any existing cached files first
        clearCachedData();
        Log.debug(LOG_TAG, "%s - Creating new cached propositions at: %s", SELF_TAG, cacheManager.getBaseFilePath(CACHE_NAME, PROPOSITIONS_CACHE_SUBDIRECTORY));
        final File propositionCache = cacheManager.createNewCacheFile(CACHE_NAME, PROPOSITIONS_CACHE_SUBDIRECTORY, new Date());
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(propositionCache, false);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(propositionPayload);
            objectOutputStream.flush();
        } catch (final IOException e) {
            Log.warning(LOG_TAG, "%s - IOException while attempting to write remote file (%s)", SELF_TAG, e);
            return;
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (final IOException e) {
                Log.warning(LOG_TAG, "%s - Unable to close the ObjectOutputStream (%s) ", SELF_TAG, e);
            }
        }
        return;
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
