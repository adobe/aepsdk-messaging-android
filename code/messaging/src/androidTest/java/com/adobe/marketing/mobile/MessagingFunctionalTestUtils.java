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

import static com.adobe.marketing.mobile.MessagingUtils.toMap;
import static com.adobe.marketing.mobile.MessagingUtils.toVariantMap;
import static com.adobe.marketing.mobile.TestConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.TestConstants.MESSAGES_CACHE_SUBDIRECTORY;
import static org.junit.Assert.fail;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MessagingFunctionalTestUtils {
    private static final String LOG_TAG = "MessagingFunctionalTestUtil";
    private final static String REMOTE_URL = "https://www.adobe.com/adobe.png";
    private static final int STREAM_WRITE_BUFFER_SIZE = 4096;
    private static CacheManager cacheManager;

    /**
     * Serialize the given {@code map} to a JSON Object, then flattens to {@code Map<String, String>}.
     * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
     * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
     *
     * @param map map with JSON structure to flatten
     * @return new map with flattened structure
     */
    static Map<String, String> flattenMap(final Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            JSONObject jsonObject = new JSONObject(map);
            Map<String, String> payloadMap = new HashMap<>();
            addKeys("", new ObjectMapper().readTree(jsonObject.toString()), payloadMap);
            return payloadMap;
        } catch (IOException e) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to parse JSON object to tree structure.");
        }

        return Collections.emptyMap();
    }

    /**
     * Deserialize {@code JsonNode} and flatten to provided {@code map}.
     * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
     * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
     * <p>
     * Method is called recursively. To use, call with an empty path such as
     * {@code addKeys("", new ObjectMapper().readTree(JsonNodeAsString), map);}
     *
     * @param currentPath the path in {@code JsonNode} to process
     * @param jsonNode    {@link JsonNode} to deserialize
     * @param map         {@code Map<String, String>} instance to store flattened JSON result
     * @see <a href="https://stackoverflow.com/a/24150263">Stack Overflow post</a>
     */
    private static void addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;

            for (int i = 0; i < arrayNode.size(); i++) {
                addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            ValueNode valueNode = (ValueNode) jsonNode;
            map.put(currentPath, valueNode.asText());
        }
    }

    /**
     * Dispatches a simulated edge response event containing a message payload. The message payload
     * is loaded from the resources directory using the passed in {@code String} as a filename.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     */
    static void dispatchEdgePersonalizationEventWithMessagePayload(final String fileName) {
        final EventData eventData = new EventData();
        final List<Variant> items = new ArrayList<>();
        items.add(Variant.fromVariantMap(getVariantMapFromFile(fileName)));
        eventData.putVariant("payload", Variant.fromVariantList(items));
        final Event event = new Event.Builder("edge response testing", TestConstants.EventType.EDGE, TestConstants.EventSource.PERSONALIZATION_DECISIONS)
                .setData(eventData)
                .build();
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.debug(LOG_TAG, "exception occurred in dispatching edge personalization event: %s", extensionError.getErrorName());
            }
        });
    }

    /**
     * Converts a file containing a JSON into a {@link Map<String, Variant>}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code Map<String, Variant>} containing the JSON's contents
     */
    static Map<String, Variant> getVariantMapFromFile(final String fileName) {
        try {
            final JSONObject json = new JSONObject(loadStringFromFile(fileName));
            return toVariantMap(json);
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "getVariantMapFromFile() - Exception occurred when creating the JSONObject: %s", jsonException.getMessage());
            return null;
        }
    }

    /**
     * Converts a file containing a JSON into a {@link Map<String, Object>}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code Map<String, Object>} containing the file's contents
     */
    static Map<String, Object> getMapFromFile(final String fileName) {
        try {
            final JSONObject json = new JSONObject(loadStringFromFile(fileName));
            return toMap(json);
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "getMapFromFile() - Exception occurred when creating the JSONObject: %s", jsonException.getMessage());
            return null;
        }
    }

    /**
     * Converts a file into a {@code String}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code String} containing the file's contents
     */
    static String loadStringFromFile(final String fileName) {
        final InputStream inputStream = MessagingFunctionalTestUtils.class.getClassLoader().getResourceAsStream(fileName);
        try {
            if (inputStream != null) {
                final String streamContents = StringUtils.streamToString(inputStream);
                return streamContents;
            } else {
                return null;
            }
        } finally {
            try {
                inputStream.close();
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, "Exception occurred when closing the input stream: %s", ioException.getMessage());
                return null;
            }
        }
    }

    /**
     * Cleans Messaging extension payload and image asset cache files.
     */
    static void cleanCache() {
        final SystemInfoService systemInfoService = MessagingUtils.getPlatformServices().getSystemInfoService();
        try {
            cacheManager = new CacheManager(systemInfoService);
        } catch (final MissingPlatformServicesException exception) {
            Log.warning(LOG_TAG, "Error clearing cache: %s", exception.getMessage());
        }
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), IMAGES_CACHE_SUBDIRECTORY);
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), MESSAGES_CACHE_SUBDIRECTORY);
    }

    /**
     * Adds a test image to the Messaging extension image asset cache.
     */
    static void addImageAssetToCache() {
        final File mockCachedImage = cacheManager.createNewCacheFile(REMOTE_URL, IMAGES_CACHE_SUBDIRECTORY, new Date());
        writeInputStreamIntoFile(mockCachedImage, MessagingFunctionalTestUtils.convertResourceFileToInputStream("adobe", ".png"), false);
    }

    /**
     * Converts a file in the resources directory into an {@link InputStream}.
     *
     * @param name          the {@code String} filename of a file located in the resource directory
     * @param fileExtension a {@code String} containing the file extension of the file
     * @return a {@code InputStream} of the specified file
     */
    static InputStream convertResourceFileToInputStream(final String name, final String fileExtension) {
        return MessagingFunctionalTestUtils.class.getClassLoader().getResourceAsStream(name + fileExtension);
    }

    /**
     * Writes the contents of an {@link InputStream} into a file.
     *
     * @param cachedFile the {@code File} to be written to
     * @param input      a {@code InputStream} containing the data to be written
     * @return a {@code boolean} if the write to file was successful
     */
    static boolean writeInputStreamIntoFile(final File cachedFile, final InputStream input, final boolean append) {
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
            Log.error(LOG_TAG, "IOException while attempting to write to file (%s)", e);
            return false;
        } catch (final Exception e) {
            Log.error(LOG_TAG, "Unexpected exception while attempting to write to file (%s)", e);
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

    /**
     * Set the persistence data for Edge Identity extension.
     */
    static void setEdgeIdentityPersistence(final Map<String, Object> persistedData) {
        if (persistedData != null) {
            final JSONObject persistedJSON = new JSONObject(persistedData);
            updatePersistence("com.adobe.edge.identity",
                    "identity.properties", persistedJSON.toString());
        }
    }

    /**
     * Helper method to update the {@link SharedPreferences} data.
     *
     * @param datastore the name of the datastore to be updated
     * @param key       the persisted data key that has to be updated
     * @param value     the new value
     */
    public static void updatePersistence(final String datastore, final String key, final String value) {
        final Application application = TestHelper.defaultApplication;

        if (application == null) {
            fail("Unable to updatePersistence by TestPersistenceHelper. Application is null, fast failing the test case.");
        }

        final Context context = application.getApplicationContext();

        if (context == null) {
            fail("Unable to updatePersistence by TestPersistenceHelper. Context is null, fast failing the test case.");
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);

        if (sharedPreferences == null) {
            fail("Unable to updatePersistence by TestPersistenceHelper. sharedPreferences is null, fast failing the test case.");
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static Map<String, Object> createIdentityMap(final String namespace, final String id) {
        Map<String, Object> namespaceObj = new HashMap<>();
        namespaceObj.put("authenticationState", "ambiguous");
        namespaceObj.put("id", id);
        namespaceObj.put("primary", false);

        List<Map<String, Object>> namespaceIds = new ArrayList<>();
        namespaceIds.add(namespaceObj);

        Map<String, List<Map<String, Object>>> identityMap = new HashMap<>();
        identityMap.put(namespace, namespaceIds);

        Map<String, Object> xdmMap = new HashMap<>();
        xdmMap.put("identityMap", identityMap);

        return xdmMap;
    }
}
