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
 * This class contains functionality to cache the json message payload and any image asset URL's
 * present in an AJO in-app message definition.
 */
final class MessagingCacheUtilities {
    private static final String SELF_TAG = "MessagingCacheUtilities";
    private final CacheService cacheService;
    private final String assetCacheLocation;
    private final String METADATA_KEY_PATH_TO_FILE = "pathToFile";
    private final Map<String, String> assetMap = new HashMap<>();

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
        return cacheService.get(
                        MessagingConstants.CACHE_BASE_DIR,
                        MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY)
                != null;
    }

    /** Delete all contents in the Messaging extension cache subdirectory. */
    @VisibleForTesting
    void clearCachedData() {
        cacheService.remove(
                MessagingConstants.CACHE_BASE_DIR,
                MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY);
        cacheService.remove(
                MessagingConstants.CACHE_BASE_DIR, MessagingConstants.IMAGES_CACHE_SUBDIRECTORY);
        Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "In-app messaging %s and %s caches have been deleted.",
                MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY,
                MessagingConstants.IMAGES_CACHE_SUBDIRECTORY);
    }

    /**
     * Retrieves cached {@code String} proposition payloads and returns them in a {@link List<
     * Proposition >}.
     *
     * @return a {@code Map<Surface, List<Proposition>>} containing the cached proposition payloads.
     */
    Map<Surface, List<Proposition>> getCachedPropositions() {
        final CacheResult cacheResult =
                cacheService.get(
                        MessagingConstants.CACHE_BASE_DIR,
                        MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY);
        if (cacheResult == null) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to find a cached proposition.");
            return null;
        }

        final Map<String, String> fileMetadata = cacheResult.getMetadata();
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Loading cached proposition from (%s)",
                    fileMetadata.get(METADATA_KEY_PATH_TO_FILE));
        }

        ObjectInputStream objectInputStream = null;
        Map<Surface, List<Proposition>> cachedPropositions = new HashMap<>();
        try {
            objectInputStream = new ObjectInputStream(cacheResult.getData());

            final Object cachedData = objectInputStream.readObject();
            if (cachedData == null) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to read cached data into an object.");
                return null;
            }

            Object firstElement;
            try {
                firstElement =
                        ((Map<Surface, List<Object>>) cachedData)
                                .entrySet()
                                .iterator()
                                .next()
                                .getValue()
                                .get(0);
            } catch (final NoSuchElementException exception) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to retrieve first element of cached data list.");
                return null;
            }

            // handle cached Proposition objects
            if (firstElement instanceof Proposition) {
                cachedPropositions = (Map<Surface, List<Proposition>>) cachedData;
            } else if (firstElement instanceof PropositionPayload) {
                // handle cached PropositionPayload objects
                final Map<Surface, List<PropositionPayload>> cachedPropositionPayloads =
                        (Map<Surface, List<PropositionPayload>>) cachedData;
                for (final Map.Entry<Surface, List<PropositionPayload>> entry :
                        cachedPropositionPayloads.entrySet()) {
                    cachedPropositions.put(entry.getKey(), convertToPropositions(entry.getValue()));
                }
            }
        } catch (final NullPointerException nullPointerException) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred when retrieving the cached proposition file: %s",
                    nullPointerException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred when reading from the cached file: %s",
                    ioException.getMessage());
            return null;
        } catch (final ClassNotFoundException classNotFoundException) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Class not found: %s",
                    classNotFoundException.getMessage());
            return null;
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Exception occurred when closing the FileInputStream: %s",
                        ioException.getMessage());
            }
        }
        return cachedPropositions;
    }

    /**
     * Caches the provided {@code Map<Surface, List<Proposition>>}.
     *
     * @param newPropositions the {@link Map<Surface, List< Proposition >>} containing the
     *     propositions to be cached.
     * @param surfacesToRemove {@link List<Surface>} containing surfaces to be removed from the
     *     cache
     */
    void cachePropositions(
            final Map<Surface, List<Proposition>> newPropositions,
            final List<Surface> surfacesToRemove) {
        final Map<Surface, List<Proposition>> cachedPropositions = getCachedPropositions();
        final Map<Surface, List<Proposition>> updatedPropositions =
                cachedPropositions != null ? cachedPropositions : new HashMap<>();
        updatedPropositions.putAll(newPropositions);
        final List<Surface> propositionsToRemove = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> entry : updatedPropositions.entrySet()) {
            if (surfacesToRemove.contains(entry.getKey())) {
                propositionsToRemove.add(entry.getKey());
            }
        }
        for (final Surface surface : propositionsToRemove) {
            updatedPropositions.remove(surface);
        }

        // clean any existing cached propositions first if the provided propositions are null or
        // empty
        final Map<Surface, List<Proposition>> propositions = new HashMap<>(updatedPropositions);
        if (MapUtils.isNullOrEmpty(propositions)) {
            cacheService.remove(
                    MessagingConstants.CACHE_BASE_DIR,
                    MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY);
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "In-app messaging cache has been deleted.");
            return;
        }

        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Creating new cached propositions");
        ByteArrayOutputStream byteArrayOutputStream = null;
        InputStream inputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(propositions);
            objectOutputStream.flush();
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            final CacheEntry cacheEntry = new CacheEntry(inputStream, CacheExpiry.never(), null);
            cacheService.set(
                    MessagingConstants.CACHE_BASE_DIR,
                    MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY,
                    cacheEntry);
        } catch (final IOException e) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "IOException while attempting to write remote file (%s)",
                    e);
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
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to close the ObjectOutputStream (%s) ",
                        e);
            }
        }
    }

    /**
     * Converts the provided {@code PropositionPayload} into a {@code Proposition}.
     *
     * @param propositionPayloads {@link List<PropositionPayload>} to be converted
     * @return a {@link List< Proposition >} created from the provided {@code PropositionPayload}
     */
    private List<Proposition> convertToPropositions(
            final List<PropositionPayload> propositionPayloads) {
        final List<Proposition> propositions = new ArrayList<>();
        final List<PropositionItem> propositionItems = new ArrayList<>();
        try {
            for (final PropositionPayload propositionPayload : propositionPayloads) {
                for (final PayloadItem payloadItem : propositionPayload.items) {
                    final PropositionItem propositionItem =
                            new PropositionItem(
                                    payloadItem.id,
                                    SchemaType.fromString(payloadItem.schema),
                                    payloadItem.data);
                    propositionItems.add(propositionItem);
                }
                propositions.add(
                        new Proposition(
                                propositionPayload.propositionInfo.id,
                                propositionPayload.propositionInfo.scope,
                                propositionPayload.propositionInfo.scopeDetails,
                                propositionItems));
            }
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating Proposition: %s",
                    exception.getLocalizedMessage());
        }
        return propositions;
    }

    // ========================================================================================================
    // Private helpers for proposition caching
    // ========================================================================================================

    /**
     * Retrieves cached propositions for the given cache key.
     *
     * @param cacheKey the cache subdirectory key to retrieve from
     * @param notFoundMessage log message to show when no cached data is found
     * @return a {@code Map<Surface, List<Proposition>>} of cached propositions, or null if none
     *     found
     */
    private Map<Surface, List<Proposition>> getCachedPropositionsForKey(
            final String cacheKey, final String notFoundMessage) {
        final CacheResult cacheResult =
                cacheService.get(MessagingConstants.CACHE_BASE_DIR, cacheKey);
        if (cacheResult == null) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, notFoundMessage);
            return null;
        }

        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(cacheResult.getData());
            final Object cachedData = objectInputStream.readObject();
            if (cachedData == null) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to read cached data into an object for key: %s",
                        cacheKey);
                return null;
            }

            // cached data should be Map<Surface, List<Proposition>>
            if (cachedData instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<Surface, List<Proposition>> result =
                        (Map<Surface, List<Proposition>>) cachedData;
                return result;
            }

            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cached data for key %s is not in the expected format.",
                    cacheKey);
            return null;
        } catch (final IOException ioException) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred when reading cached data for key %s: %s",
                    cacheKey,
                    ioException.getMessage());
            return null;
        } catch (final ClassNotFoundException classNotFoundException) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Class not found when reading cached data for key %s: %s",
                    cacheKey,
                    classNotFoundException.getMessage());
            return null;
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Exception occurred when closing ObjectInputStream for key %s: %s",
                        cacheKey,
                        ioException.getMessage());
            }
        }
    }

    /**
     * Caches propositions under the given cache key, merging with existing cached data.
     *
     * @param cacheKey the cache subdirectory key to write to
     * @param newPropositions the new propositions to cache
     * @param surfacesToRemove surfaces to remove from the cache
     * @param logPrefix prefix for log messages
     */
    private void cachePropositionsForKey(
            final String cacheKey,
            final Map<Surface, List<Proposition>> newPropositions,
            final List<Surface> surfacesToRemove,
            final String logPrefix) {
        final Map<Surface, List<Proposition>> cachedPropositions =
                getCachedPropositionsForKey(cacheKey, logPrefix + " - no existing cache found.");
        final Map<Surface, List<Proposition>> updatedPropositions =
                cachedPropositions != null ? cachedPropositions : new HashMap<>();
        updatedPropositions.putAll(newPropositions);

        // remove surfaces that should be evicted
        if (!MessagingUtils.isNullOrEmpty(surfacesToRemove)) {
            for (final Surface surface : surfacesToRemove) {
                updatedPropositions.remove(surface);
            }
        }

        final Map<Surface, List<Proposition>> propositions = new HashMap<>(updatedPropositions);
        if (MapUtils.isNullOrEmpty(propositions)) {
            cacheService.remove(MessagingConstants.CACHE_BASE_DIR, cacheKey);
            Log.trace(
                    MessagingConstants.LOG_TAG, SELF_TAG, "%s cache has been deleted.", logPrefix);
            return;
        }

        Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Creating new cached %s propositions",
                logPrefix);
        ByteArrayOutputStream byteArrayOutputStream = null;
        InputStream inputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(propositions);
            objectOutputStream.flush();
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            final CacheEntry cacheEntry = new CacheEntry(inputStream, CacheExpiry.never(), null);
            cacheService.set(MessagingConstants.CACHE_BASE_DIR, cacheKey, cacheEntry);
        } catch (final IOException e) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "IOException while attempting to write %s cache (%s)",
                    logPrefix,
                    e);
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
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to close streams for %s cache (%s)",
                        logPrefix,
                        e);
            }
        }
    }

    // ========================================================================================================
    // Content card proposition caching
    // ========================================================================================================

    /**
     * Retrieves cached content card propositions.
     *
     * @return a {@code Map<Surface, List<Proposition>>} containing cached content card
     *     propositions, or null if none found.
     */
    Map<Surface, List<Proposition>> getCachedContentCardPropositions() {
        return getCachedPropositionsForKey(
                MessagingConstants.CONTENT_CARD_PROPOSITIONS_CACHE_SUBDIRECTORY,
                "Unable to find cached content card propositions.");
    }

    /**
     * Caches content card propositions.
     *
     * @param newPropositions the content card propositions to cache
     * @param surfacesToRemove surfaces to remove from the content card cache
     */
    void cacheContentCardPropositions(
            final Map<Surface, List<Proposition>> newPropositions,
            final List<Surface> surfacesToRemove) {
        cachePropositionsForKey(
                MessagingConstants.CONTENT_CARD_PROPOSITIONS_CACHE_SUBDIRECTORY,
                newPropositions,
                surfacesToRemove,
                "Content card");
    }

    // ========================================================================================================
    // Inbox proposition caching
    // ========================================================================================================

    /**
     * Retrieves cached inbox propositions.
     *
     * @return a {@code Map<Surface, List<Proposition>>} containing cached inbox propositions, or
     *     null if none found.
     */
    Map<Surface, List<Proposition>> getCachedInboxPropositions() {
        return getCachedPropositionsForKey(
                MessagingConstants.INBOX_PROPOSITIONS_CACHE_SUBDIRECTORY,
                "Unable to find cached inbox propositions.");
    }

    /**
     * Caches inbox propositions.
     *
     * @param newPropositions the inbox propositions to cache
     * @param surfacesToRemove surfaces to remove from the inbox cache
     */
    void cacheInboxPropositions(
            final Map<Surface, List<Proposition>> newPropositions,
            final List<Surface> surfacesToRemove) {
        cachePropositionsForKey(
                MessagingConstants.INBOX_PROPOSITIONS_CACHE_SUBDIRECTORY,
                newPropositions,
                surfacesToRemove,
                "Inbox");
    }

    /**
     * Clears all persisted content card and inbox proposition caches. Does not affect the IAM
     * propositions cache.
     */
    void clearPersistedContentCardAndInboxCaches() {
        cacheService.remove(
                MessagingConstants.CACHE_BASE_DIR,
                MessagingConstants.CONTENT_CARD_PROPOSITIONS_CACHE_SUBDIRECTORY);
        cacheService.remove(
                MessagingConstants.CACHE_BASE_DIR,
                MessagingConstants.INBOX_PROPOSITIONS_CACHE_SUBDIRECTORY);
        Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Content card and inbox proposition caches have been deleted.");
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
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to cache asset, the asset cache location is not available.");
            return;
        }

        if (cacheService == null) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to cache asset, the cache manager is not available.");
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
     *
     * <p>Checks that the provided asset is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param asset {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    private boolean assetIsDownloadable(final String asset) {
        return UrlUtils.isValidUrl(asset)
                && (asset.startsWith("http") || asset.startsWith("https"));
    }

    /**
     * Returns a {@link Map<String, String>} containing the remote asset mapped to it's cached
     * location.
     *
     * @return {@code Map<String, String} containing a mapping of a remote image asset URL and it's
     *     cached location
     */
    Map<String, String> getAssetsMap() {
        return assetMap;
    }
}
