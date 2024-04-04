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

import com.adobe.marketing.mobile.internal.util.StringEncoder;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Assists in downloading and caching assets for {@code Message}s. */
class MessageAssetDownloader {
    private static final String SELF_TAG = "MessageAssetDownloader";
    private final List<String> assetsCollection;
    private final CacheService cacheService;
    private final String assetCacheLocation;
    private File assetDir;

    /**
     * Constructor.
     *
     * @param assets {@code ArrayList<String>} of assets to download and cache
     */
    MessageAssetDownloader(final List<String> assets) {
        this.assetsCollection = assets;
        this.cacheService = ServiceProvider.getInstance().getCacheService();
        this.assetCacheLocation = InternalMessagingUtils.getAssetCacheLocation();
        createAssetCacheDirectory();
    }

    /**
     * Downloads and caches all assets present in the {@link
     * MessageAssetDownloader#assetsCollection} list.
     *
     * <p>Attempts to purge assets that have previously been cached but are for messages that are no
     * longer active.
     */
    void downloadAssetCollection() {
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "downloadAssetCollection - Failed to download assets, the asset cache location"
                            + " is not available.");
            return;
        }

        if (assetsCollection == null || assetsCollection.isEmpty()) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "downloadAssetCollection - Empty list of assets provided, will not download any"
                            + " assets.");
            return;
        }

        // clear old assets
        if (assetDir != null) {
            clearCachedAssetsNotInList(assetDir, assetsCollection);
        }

        // download assets within the assets collection list
        for (final String url : assetsCollection) {
            // 304 - Not Modified support
            final CacheResult cachedAsset = cacheService.get(assetCacheLocation, url);
            final Map<String, String> requestProperties = extractHeadersFromCache(cachedAsset);
            final NetworkRequest networkRequest =
                    new NetworkRequest(
                            url,
                            HttpMethod.GET,
                            null,
                            requestProperties,
                            MessagingConstants.DEFAULT_TIMEOUT,
                            MessagingConstants.DEFAULT_TIMEOUT);
            ServiceProvider.getInstance()
                    .getNetworkService()
                    .connectAsync(
                            networkRequest,
                            connection -> {
                                if (connection == null) {
                                    Log.warning(
                                            MessagingConstants.LOG_TAG,
                                            SELF_TAG,
                                            "downloadAssetCollection - connection returned from"
                                                    + " NetworkService was null. Aborting asset"
                                                    + " download for: %s",
                                            url);
                                    return;
                                }
                                if (connection.getResponseCode()
                                        == HttpURLConnection.HTTP_NOT_MODIFIED) {
                                    Log.debug(
                                            MessagingConstants.LOG_TAG,
                                            SELF_TAG,
                                            "downloadAssetCollection - Asset was cached previously:"
                                                    + " %s",
                                            url);
                                    connection.close();
                                    return;
                                } else if (connection.getResponseCode()
                                        != HttpURLConnection.HTTP_OK) {
                                    Log.debug(
                                            MessagingConstants.LOG_TAG,
                                            SELF_TAG,
                                            "downloadAssetCollection - Failed to download asset"
                                                    + " from URL: %s",
                                            url);
                                    connection.close();
                                    return;
                                }
                                cacheAssetData(connection, url);
                                connection.close();
                            });
        }
    }

    /**
     * Recursively checks and deletes files within the cached assets directory which aren't within
     * the {@code assetsToRetain} list.
     *
     * @param cacheAsset {@link File} containing the cached assets directory
     * @param assetsToRetain {@code List<String>} containing assets which should be retained
     */
    private void clearCachedAssetsNotInList(
            final File cacheAsset, final List<String> assetsToRetain) {
        if (cacheAsset.isDirectory()) {
            for (final File child : cacheAsset.listFiles()) {
                clearCachedAssetsNotInList(child, assetsToRetain);
            }
        } else {
            for (final String asset : assetsToRetain) {
                if (!cacheAsset.getName().equals(StringEncoder.sha2hash(asset))
                        && cacheAsset.exists()) {
                    cacheAsset.delete();
                }
            }
        }
    }

    /**
     * Extracts the response properties (like {@code HTTP_HEADER_ETAG} , {@code
     * HTTP_HEADER_LAST_MODIFIED} that are useful as cache metadata.
     *
     * @param response the {@code HttpConnecting} from where the response properties should be
     *     extracted from
     * @return a map of metadata keys and their values as obrained from the {@code response}
     */
    private HashMap<String, String> extractMetadataFromResponse(final HttpConnecting response) {
        final HashMap<String, String> metadata = new HashMap<>();

        final String lastModifiedProp =
                response.getResponsePropertyValue(MessagingConstants.HTTP_HEADER_LAST_MODIFIED);
        final Date lastModifiedDate =
                TimeUtils.parseRFC2822Date(
                        lastModifiedProp, TimeZone.getTimeZone("GMT"), Locale.US);
        final String lastModifiedMetadata =
                lastModifiedDate == null
                        ? String.valueOf(new Date(0L).getTime())
                        : String.valueOf(lastModifiedDate.getTime());
        metadata.put(MessagingConstants.HTTP_HEADER_LAST_MODIFIED, lastModifiedMetadata);

        final String eTagProp =
                response.getResponsePropertyValue(MessagingConstants.HTTP_HEADER_ETAG);
        metadata.put(MessagingConstants.HTTP_HEADER_ETAG, eTagProp == null ? "" : eTagProp);

        return metadata;
    }

    /**
     * Creates http headers for conditional fetching, based on the metadata of the {@code
     * CacheResult} provided.
     *
     * @param cacheResult the cache result whose metadata should be used for finding headers
     * @return a map of headers (HTTP_HEADER_IF_MODIFIED_SINCE, HTTP_HEADER_IF_NONE_MATCH) that can
     *     be used while fetching any modified content.
     */
    private Map<String, String> extractHeadersFromCache(final CacheResult cacheResult) {
        final Map<String, String> headers = new HashMap<>();
        if (cacheResult == null) {
            return headers;
        }

        final Map<String, String> metadata = cacheResult.getMetadata();
        final String eTag =
                metadata == null ? "" : metadata.get(MessagingConstants.HTTP_HEADER_ETAG);
        headers.put(MessagingConstants.HTTP_HEADER_IF_NONE_MATCH, eTag != null ? eTag : "");

        // Last modified in cache metadata is stored in epoch string. So Convert it to RFC-2822 date
        // format.
        final String lastModified =
                metadata == null
                        ? null
                        : metadata.get(MessagingConstants.HTTP_HEADER_LAST_MODIFIED);
        long lastModifiedEpoch;
        try {
            lastModifiedEpoch = lastModified != null ? Long.parseLong(lastModified) : 0L;
        } catch (final NumberFormatException e) {
            lastModifiedEpoch = 0L;
        }

        final String ifModifiedSince =
                TimeUtils.getRFC2822Date(lastModifiedEpoch, TimeZone.getTimeZone("GMT"), Locale.US);
        headers.put(MessagingConstants.HTTP_HEADER_IF_MODIFIED_SINCE, ifModifiedSince);
        return headers;
    }

    /**
     * Caches the provided {@code InputStream} contained in the {@code HttpConnecting} from the
     * given asset URL.
     *
     * @param connection {@link HttpConnecting} containing the downloaded remote asset data.
     * @param key {@code String} The asset download URL.
     */
    private void cacheAssetData(final HttpConnecting connection, final String key) {
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "cacheAssetData - Failed to cache asset from %s, the asset cache location is"
                            + " not available.",
                    key);
            return;
        }

        // create message asset cache directory if needed
        if (!createAssetCacheDirectory()) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "cacheAssetData - Cannot cache asset, failed to create image cache directory.");
            return;
        }

        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "cacheAssetData - Caching asset %s.", key);
        final Map<String, String> metadata = extractMetadataFromResponse(connection);
        final CacheEntry cacheEntry =
                new CacheEntry(connection.getInputStream(), CacheExpiry.never(), metadata);
        cacheService.set(assetCacheLocation, key, cacheEntry);
    }

    /**
     * Creates assets cache directory for a {@code Message}.
     *
     * <p>This method checks if the cache directory already exists in which case no new directory is
     * created for image assets.
     */
    private boolean createAssetCacheDirectory() {
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "createAssetCacheDirectory - Failed to create asset cache directory, the asset"
                            + " cache location is not available.");
            return false;
        }

        try {
            assetDir = new File(assetCacheLocation);
            if (!assetDir.exists()) {
                return assetDir.mkdirs();
            }
            return true;
        } catch (final Exception ex) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "createAssetCacheDirectory - An unexpected error occurred while managing the"
                            + " image cache directory: \n"
                            + " %s",
                    ex.getLocalizedMessage());
            return false;
        }
    }
}
