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

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class contains functionality to cache the json message payload and any image asset URL's present in an
 * AJO in-app message definition.
 */
final class MessagingCacheUtilities {
    private final static String SELF_TAG = "MessagingCacheUtilities";
    private final CacheService cacheService;
    private final String assetCacheLocation;
    private final String METADATA_KEY_PATH_TO_FILE = "pathToFile";
    private final Map<String, String> assetMap = new HashMap<>();
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public MessagingCacheUtilities() {
        this.cacheService = ServiceProvider.getInstance().getCacheService();
        this.assetCacheLocation = InternalMessagingUtils.getAssetCacheLocation();
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
        return cacheService.get(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY) != null;
    }

    /**
     * Delete all contents in the Messaging extension cache subdirectory.
     */
    @VisibleForTesting
    void clearCachedData() {
        cacheService.remove(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
        cacheService.remove(MessagingConstants.CACHE_BASE_DIR, IMAGES_CACHE_SUBDIRECTORY);
        Log.trace(LOG_TAG, SELF_TAG, "In-app messaging %s and %s caches have been deleted.", PROPOSITIONS_CACHE_SUBDIRECTORY, IMAGES_CACHE_SUBDIRECTORY);
    }

    /**
     * Retrieves cached {@code String} proposition payloads and returns them in a {@link List< Proposition >}.
     *
     * @return a {@code Map<Surface, List<Proposition>>} containing the cached proposition payloads.
     */
    Map<Surface, List<Proposition>> getCachedPropositions() {
        final CacheResult cacheResult = cacheService.get(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
        if (cacheResult == null) {
            Log.trace(LOG_TAG, SELF_TAG, "Unable to find a cached proposition.");
            return null;
        }

        final Map<String, String> fileMetadata = cacheResult.getMetadata();
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            Log.trace(LOG_TAG, SELF_TAG, "Loading cached proposition from (%s)", fileMetadata.get(METADATA_KEY_PATH_TO_FILE));
        }

        ObjectInputStream objectInputStream = null;
        Map<Surface, List<Proposition>> cachedPropositions = new HashMap<>();
        try {
            if (objectInputStream == null) {
                objectInputStream = new ObjectInputStream(cacheResult.getData());
            }

            final Object cachedData = objectInputStream.readObject();
            if (cachedData == null) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to read cached data into an object.");
                return null;
            }

            Object firstElement;
            try {
                firstElement = ((Map<Surface, List<Object>>) cachedData).entrySet().iterator().next().getValue().get(0);
            } catch (final NoSuchElementException exception) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to retrieve first element of cached data list.");
                return null;
            }

            // handle cached Proposition objects
            if (firstElement instanceof Proposition) {
                cachedPropositions = (Map<Surface, List<Proposition>>) cachedData;
            } else if (firstElement instanceof PropositionPayload) {
                // handle cached PropositionPayload objects
                final Map<Surface, List<PropositionPayload>> cachedPropositionPayloads = (Map<Surface, List<PropositionPayload>>) cachedData;
                if (!MapUtils.isNullOrEmpty(cachedPropositionPayloads)) {
                    for (final Map.Entry<Surface, List<PropositionPayload>> entry : cachedPropositionPayloads.entrySet()) {
                        cachedPropositions.put(entry.getKey(), convertToPropositions(entry.getValue()));
                    }
                }
            }
        } catch (final NullPointerException nullPointerException) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when retrieving the cached proposition file: %s", nullPointerException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when reading from the cached file: %s", ioException.getMessage());
            return null;
        } catch (final ClassNotFoundException classNotFoundException) {
            Log.warning(LOG_TAG, SELF_TAG, "Class not found: %s", classNotFoundException.getMessage());
            return null;
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when closing the FileInputStream: %s", ioException.getMessage());
            }
        }
        return cachedPropositions;
    }

    /**
     * Caches the provided {@code Map<Surface, List<Proposition>>}.
     *
     * @param newPropositions the {@link Map<Surface, List< Proposition >>} containing the propositions to be cached.
     * @param surfacesToRemove {@link List<Surface>} containing surfaces to be removed from the cache
     */
    void cachePropositions(final Map<Surface, List<Proposition>> newPropositions, final List<Surface> surfacesToRemove) {
        final Map<Surface, List<Proposition>> cachedPropositions = getCachedPropositions();
        final Map<Surface, List<Proposition>> updatedPropositions = cachedPropositions != null ? cachedPropositions : new HashMap<>();
        updatedPropositions.putAll(newPropositions);
        for (final Map.Entry<Surface, List<Proposition>> entry : updatedPropositions.entrySet()) {
            if (surfacesToRemove.contains(entry.getKey())) {
                updatedPropositions.remove(entry);
            }
        }

        // clean any existing cached propositions first if the provided propositions are null or empty
        final Map<Surface, List<Proposition>> propositions = new HashMap<>(updatedPropositions);
        if (MapUtils.isNullOrEmpty(propositions)) {
            cacheService.remove(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "In-app messaging cache has been deleted.");
            return;
        }

        Log.debug(LOG_TAG, SELF_TAG, "Creating new cached propositions");
        ByteArrayOutputStream byteArrayOutputStream = null;
        InputStream inputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            if (objectOutputStream == null) {
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            }
            objectOutputStream.writeObject(propositions);
            objectOutputStream.flush();
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            final CacheEntry cacheEntry = new CacheEntry(inputStream, CacheExpiry.never(), null);
            cacheService.set(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY, cacheEntry);
        } catch (final IOException e) {
            Log.warning(LOG_TAG, SELF_TAG, "IOException while attempting to write remote file (%s)", e);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException e) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to close the ObjectOutputStream (%s) ", e);
            }
        }
    }

    /**
     * Converts the provided {@code PropositionPayload} into a {@code Proposition}.
     *
     * @param propositionPayloads {@link List<PropositionPayload>} to be converted
     * @return a {@link List< Proposition >} created from the provided {@code PropositionPayload}
     */
    private List<Proposition> convertToPropositions(final List<PropositionPayload> propositionPayloads) {
        final List<Proposition> propositions = new ArrayList<>();
        final List<PropositionItem> propositionItems = new ArrayList<>();
        for (final PropositionPayload propositionPayload : propositionPayloads) {
            for (final PayloadItem payloadItem : propositionPayload.items) {
                final PropositionItem propositionItem = new PropositionItem(payloadItem.id, SchemaType.fromString(payloadItem.schema), payloadItem.data);
                propositionItems.add(propositionItem);
            }
            propositions.add(new Proposition(propositionPayload.propositionInfo.id, propositionPayload.propositionInfo.scope, propositionPayload.propositionInfo.scopeDetails, propositionItems));
        }
        return propositions;
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
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(LOG_TAG, SELF_TAG, "Failed to cache asset, the asset cache location is not available.");
            return;
        }

        if (cacheService == null) {
            Log.trace(LOG_TAG, SELF_TAG, "Failed to cache asset, the cache manager is not available.");
            return;
        }

        final List<String> assetsToRetain = new ArrayList<>();

        // validate asset URLs and remove duplicates
        if (!MessagingUtils.isNullOrEmpty(assetsUrls)) {
            for (final String imageAssetUrl : assetsUrls) {
                if (assetIsDownloadable(imageAssetUrl) && !assetsToRetain.contains(imageAssetUrl)) {
                    assetsToRetain.add(imageAssetUrl);
                    // update the asset to cached location map
                    assetMap.put(imageAssetUrl, assetCacheLocation);
                }
            }
        }

        // download the assets
        MessageAssetDownloader messageAssetDownloader = new MessageAssetDownloader(assetsToRetain);
        messageAssetDownloader.downloadAssetCollection();
    }

    /**
     * Determine whether the provided {@code String} asset is downloadable.
     * <p>
     * Checks that the provided asset is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param asset {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    private boolean assetIsDownloadable(final String asset) {
        return UrlUtils.isValidUrl(asset) && (asset.startsWith("http") || asset.startsWith("https"));
    }

    /**
     * Returns a {@link Map<String, String>} containing the remote asset mapped to it's cached location.
     *
     * @return {@code Map<String, String} containing a mapping of a remote image asset URL and it's cached location
     */
    Map<String, String> getAssetsMap() {
        return assetMap;
    }

    @VisibleForTesting
    void setObjectInputStream(final ObjectInputStream objectInputStream) {
        this.objectInputStream = objectInputStream;
    }

    @VisibleForTesting
    void setObjectOutputStream(final ObjectOutputStream objectOutputStream) {
        this.objectOutputStream = objectOutputStream;
    }
}