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

package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.internal.caching.FileCacheService;
import com.adobe.marketing.mobile.util.UrlUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assists in downloading and caching assets for {@code CampaignMessage}s.
 */
class MessageAssetDownloader {
    private static final String SELF_TAG = "CampaignMessageAssetsDownloader";
    private final List<String> assetsCollection;
    private final Networking networkService;
    private final DeviceInforming deviceInfoService;
    private final CacheService cacheService;
    private final String messageId;
    private File assetDir;
    private final String assetCacheLocation;

    /**
     * Constructor.
     *
     * @param assets          {@code ArrayList<String>} of assets to download and cache
     * @param parentMessageId {@link String} containing the message Id of the requesting message used as a cache subdirectory
     */
    CampaignMessageAssetsDownloader(final List<String> assets, final String parentMessageId) {
        this.assetsCollection = assets;
        this.networkService = ServiceProvider.getInstance().getNetworkService();
        this.deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
        this.cacheService = new FileCacheService();
        this.messageId = parentMessageId;
        this.assetCacheLocation =  CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.MESSAGE_CACHE_DIR + File.separator + messageId;
        createMessageAssetCacheDirectory();
    }

    /**
     * Downloads and caches assets for a {@code CampaignMessage}.
     * <p>
     * Loops through {@link #assetsCollection} downloads and caches the collection of assets.
     * <p>
     * Attempts to purge assets that have previously been cached but are for messages that are no longer active.
     */
    void downloadAssetCollection() {
        final ArrayList<String> assetsToRetain = new ArrayList<>();

        if (assetsCollection != null && !assetsCollection.isEmpty()) {
            for (final String currentAsset : assetsCollection) {
                if (assetIsDownloadable(currentAsset)) {
                    assetsToRetain.add(currentAsset);
                }
            }
        }

        // clear old assets
        Utils.clearCachedAssetsNotInList(assetDir, assetsToRetain);

        // download assets within the assets to retain list
        for (final String url : assetsToRetain) {
            // 304 - Not Modified support
            final CacheResult cachedAsset = cacheService.get(assetCacheLocation, url);
            final Map<String, String> requestProperties = Utils.extractHeadersFromCache(cachedAsset);
            final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.GET, null, requestProperties, CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT, CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT);
            networkService.connectAsync(networkRequest, connection -> {
                if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "downloadAssetCollection - Asset was cached previously: %s", url);
                    connection.close();
                    return;
                } else if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "downloadAssetCollection - Failed to download asset from URL: %s", url);
                    connection.close();
                    return;
                }
                cacheAssetData(connection, url, messageId);
                connection.close();
            });
        }
    }

    /**
     * Caches the provided {@code InputStream} contained in the {@code HttpConnecting} from the given asset URL.
     *
     * @param connection {@link HttpConnecting} containing the downloaded remote asset data.
     * @param key        {@code String} The asset download URL. Used to name the cache folder.
     * @param messageId  {@code String} The id of the message
     */
    private void cacheAssetData(final HttpConnecting connection, final String key, final String messageId) {
        // create message asset cache directory if needed
        if (!createDirectoryIfNeeded(messageId)) {
            Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "cacheAssetData - Cannot cache asset for message id %s, failed to create cache directory.", messageId);
            return;
        }

        Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "cacheAssetData - Caching asset %s for message id %s.", key, messageId);
        final Map<String, String> metadata = Utils.extractMetadataFromResponse(connection);
        final String cachePath = ServiceProvider.getInstance().getDeviceInfoService().getApplicationCacheDir().getAbsolutePath();
        metadata.put(CampaignConstants.METADATA_PATH, cachePath + File.separator + assetCacheLocation);
        final CacheEntry cacheEntry = new CacheEntry(connection.getInputStream(), CacheExpiry.never(), metadata);
        cacheService.set(assetCacheLocation, key, cacheEntry);
    }

    /**
     * Determine whether the provided {@code assetPath} is downloadable.
     * <p>
     * Checks that the {@code assetPath} is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param assetPath {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    private boolean assetIsDownloadable(final String assetPath) {
        return UrlUtils.isValidUrl(assetPath) && (assetPath.startsWith("http") || assetPath.startsWith("https"));
    }

    /**
     * Creates assets cache directory for a {@code CampaignMessage}.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     */
    private void createMessageAssetCacheDirectory() {
        try {
            assetDir = new File(deviceInfoService.getApplicationCacheDir() + File.separator + CampaignConstants.CACHE_BASE_DIR + File.separator
                    + CampaignConstants.MESSAGE_CACHE_DIR);

            if (!assetDir.exists() && !assetDir.mkdirs()) {
                Log.warning(CampaignConstants.LOG_TAG, SELF_TAG,
                        "createMessageAssetCacheDirectory - Unable to create directory for caching message assets");
            }
        } catch (final Exception ex) {
            Log.warning(CampaignConstants.LOG_TAG, SELF_TAG, "createMessageAssetCacheDirectory - An unexpected error occurred while managing assets cache directory: \n %s", ex);
        }
    }

    /**
     * Creates assets cache directory for a {@code CampaignMessage}.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     *
     * @param messageId {@code String} The id of the message
     * @return {@code boolean} if true, the asset cache directory for the message id was created successfully
     */
    private boolean createDirectoryIfNeeded(final String messageId) {
        if (!assetDir.exists()) {
            return false;
        }

        final File cacheDirectory = new File(assetDir + File.separator + messageId);
        if (!cacheDirectory.exists()) {
            return cacheDirectory.mkdir();
        }
        return true;
    }
}