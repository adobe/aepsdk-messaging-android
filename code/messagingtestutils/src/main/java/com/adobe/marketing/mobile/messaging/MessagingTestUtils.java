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

import static org.junit.Assert.fail;

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.*;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheService;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.adobe.marketing.mobile.util.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import org.junit.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Util class used by both Functional and Unit tests
 */
public class MessagingTestUtils {
    private static final String LOG_TAG = "MessagingTestUtils";
    private static final String REMOTE_URL = "https://www.adobe.com/adobe.png";
    private static final int STREAM_WRITE_BUFFER_SIZE = 4096;
    static final String CHARSET_UTF_8 = "UTF-8";
    private static final int STREAM_READ_BUFFER_SIZE = 1024;

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
            Log.error(LOG_TAG, LOG_TAG, "Failed to parse JSON object to tree structure.");
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
     * Converts a file containing a JSON into a {@link Map<String, Object>}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code Map<String, Object>} containing the file's contents
     */
    public static Map<String, Object> getMapFromFile(final String fileName) {
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
        final InputStream inputStream = convertResourceFileToInputStream(fileName);
        try {
            if (inputStream != null) {
                final String streamContents = streamToString(inputStream);
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
    public static void cleanCache() {
        final CacheService cacheService = ServiceProvider.getInstance().getCacheService();
        cacheService.remove(MessagingTestConstants.CACHE_NAME, MessagingTestConstants.IMAGES_CACHE_SUBDIRECTORY);
        cacheService.remove(MessagingTestConstants.CACHE_NAME, MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY);
    }

    /**
     * Adds a test image to the Messaging extension image asset cache.
     */
    public static void addImageAssetToCache() {
        final InputStream adobePng = convertResourceFileToInputStream("adobe.png");
        final CacheEntry mockCachedImage = new CacheEntry(adobePng, CacheExpiry.never(), null);
        ServiceProvider.getInstance().getCacheService().set(MessagingTestConstants.CACHE_NAME, REMOTE_URL, mockCachedImage);
    }

    /**
     * Converts a file in the resources directory into an {@link InputStream}.
     *
     * @param filename the {@code String} filename of a file located in the resource directory
     * @return a {@code InputStream} of the specified file
     */
    static InputStream convertResourceFileToInputStream(final String filename) {
        return MessagingTestUtils.class.getClassLoader().getResourceAsStream(filename);
    }

    /**
     * Writes the contents of an {@link InputStream} into a file.
     *
     * @param cachedFile  the {@code File} to be written to
     * @param inputStream a {@code InputStream} containing the data to be written
     * @return a {@code boolean} if the write to file was successful
     */
    static boolean writeInputStreamIntoFile(final File cachedFile, final InputStream inputStream, final boolean append) {
        boolean result = false;

        if (cachedFile == null || inputStream == null) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ioException) {
                    Log.debug(LOG_TAG, "Exception occurred when closing input stream: %s", ioException.getMessage());
                }
            }
            return result;
        }

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
                outputStream.flush();
            }
            result = true;
        } catch (final IOException e) {
            Log.error(LOG_TAG, LOG_TAG, "IOException while attempting to write to file (%s)", e.getLocalizedMessage());
        } catch (final Exception e) {
            Log.error(LOG_TAG, LOG_TAG, "Unexpected exception while attempting to write to file (%s)", e.getLocalizedMessage());
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (final Exception e) {
                Log.error(LOG_TAG, LOG_TAG, "Unable to close the OutputStream (%s) ", e.getLocalizedMessage());
            }
        }

        return result;
    }

    /**
     * Set the persistence data for Edge Identity extension.
     */
    public static void setEdgeIdentityPersistence(final Map<String, Object> persistedData, final Application application) {
        if (persistedData != null) {
            final JSONObject persistedJSON = new JSONObject(persistedData);
            updatePersistence("com.adobe.edge.identity",
                    "identity.properties", persistedJSON.toString(), application);
        }
    }

    /**
     * Helper method to update the {@link SharedPreferences} data.
     *
     * @param datastore   the name of the datastore to be updated
     * @param key         the persisted data key that has to be updated
     * @param value       the new value
     * @param application the current test application
     */
    public static void updatePersistence(final String datastore, final String key, final String value, final Application application) {
        if (application == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. Application is null, fast failing the test case.");
        }

        final Context context = application.getApplicationContext();

        if (context == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. Context is null, fast failing the test case.");
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);

        if (sharedPreferences == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. sharedPreferences is null, fast failing the test case.");
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static Map<String, Object> createIdentityMap(final String namespace, final String id) {
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

    static List<PropositionPayload> getPropositionPayloadsFromMaps(final List<Map<String, Object>> payloads) throws Exception {
        final List<PropositionPayload> propositionPayloads = new ArrayList<>();
        for (final Map<String, Object> payload : payloads) {
            if (payload != null) {
                final PropositionInfo propositionInfo = PropositionInfo.create(payload);
                final PropositionPayload propositionPayload = PropositionPayload.create(propositionInfo, (List<Map<String, Object>>) payload.get("items"));
                if (propositionPayload != null) {
                    propositionPayloads.add(propositionPayload);
                }
            }
        }
        return propositionPayloads;
    }

    static PropositionInfo generatePropositionInfo(boolean nullScopeDetails) throws Exception {
        Map<String, Object> scopeDetails = new HashMap<>();
        Map<String, Object> characteristics = new HashMap<>();
        Map<String, Object> cjmEvent = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();
        Map<String, Object> messagePayload = new HashMap<>();
        messageExecution.put("messageExecutionID", "testExecutionId");
        cjmEvent.put("messageExecution", messageExecution);
        characteristics.put("cjmEvent", cjmEvent);
        scopeDetails.put("scopeDetails", characteristics);
        if (nullScopeDetails) {
            messagePayload.put("scopeDetails", null);
        } else {
            messagePayload.put("scopeDetails", scopeDetails);
        }
        messagePayload.put("scope", "mobileapp://mockPackageName");
        messagePayload.put("id", "testResponseId");
        return PropositionInfo.create(messagePayload);
    }

    static List<Map<String, Object>> generateMessagePayload(final MessageTestConfig config) {
        final Random random = new Random();
        if (config.count <= 0) {
            return null;
        }
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> scopeDetails = new HashMap<>();
        int count;
        for (count = 0; count < config.count; count++) {
            Map<String, Object> item = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> characteristics = new HashMap<>();
            Map<String, Object> cjmEvent = new HashMap<>();
            Map<String, Object> messageExecution = new HashMap<>();
            item.put("schema", "https://ns.adobe.com/personalization/ruleset-item");
            item.put("id", "testItemId" + count);
            messageExecution.put("messageExecutionID", "testExecutionId");
            cjmEvent.put("messageExecution", messageExecution);
            characteristics.put("cjmEvent", cjmEvent);
            scopeDetails.put("scopeDetails", characteristics);
            final int randomInt = random.nextInt(999999);
            data.put("id", "a96f091a-d3c6-46e0-84e0-1059d9" + randomInt);
            data.put("content", "{\"version\": 1 , " + (config.isMissingRulesKey ? "\"invalid\"" : "\"rules\"") + ": [{\"condition\":{\"type\":\"matcher\",\"definition\":{\"key\":\"isLoggedIn" + count + "\",\"matcher\":\"eq\",\"values\":[\"true\"]}},\"consequences\":[{" + (config.isMissingMessageId ? "" : "\"id\":\"fa99415e-dc8b-478a-84d2-21f67d" + randomInt + "\",") + (config.isMissingMessageType ? "" : "\"type\":\"cjmiam\",") + (config.isMissingMessageDetail ? "" : "\"detail\":{\"mobileParameters\":{\"schemaVersion\":\"0.0.1\",\"width\":100,\"height\":100,\"verticalAlign\":\"center\",\"verticalInset\":0,\"horizontalAlign\":\"center\",\"horizontalInset\":0,\"uiTakeover\":true,\"displayAnimation\":\"bottom\",\"dismissAnimation\":\"bottom\",\"gestures\":{\"swipeDown\":\"adbinapp://dismiss?interaction=swipeDown\",\"swipeUp\":\"adbinapp://dismiss?interaction=swipeUp\"}},") + (config.hasHtmlPayloadMissing ? "" : "\"html\":\"<html><head></head><body>Hello from InApp campaign: [CIT]::inapp::LqhnZy7y1Vo4EEWciU5qK</body></html>\",") + "\"remoteAssets\":[\"https://www.adobe.com/adobe.png\"]}}]}]}");
            item.put("data", data);
            items.add(item);
        }
        Map<String, Object> messagePayload = new HashMap<>();

        // scope details modification
        if (!config.isMissingScopeDetails) {
            messagePayload.put("scopeDetails", scopeDetails);
        }

        // scope modification
        if (!config.noValidAppSurfaceInPayload) {
            messagePayload.put("scope", "mobileapp://mockPackageName");
        } else if (config.nonMatchingAppSurfaceInPayload) {
            messagePayload.put("scope", "mobileapp://invalidId");
        }

        if (config.isMissingScope) {
            messagePayload.remove("scope");
        }

        messagePayload.put("items", items);
        messagePayload.put("id", "testResponseId" + count);
        List<Map<String, Object>> payload = new ArrayList<>();
        if (!config.hasEmptyPayload) {
            payload.add(messagePayload);
        }
        return payload;
    }

    static List<Map<String, Object>> generateFeedPayload(final MessageTestConfig config) {
        if (config.count <= 0) {
            return null;
        }
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> scopeDetails = new HashMap<>();

        Map<String, Object> item = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> characteristics = new HashMap<>();
        Map<String, Object> cjmEvent = new HashMap<>();
        Map<String, Object> messageExecution = new HashMap<>();

        // generate consequences containing "x = count" number of feed items
        List<JSONArray> consequences = new ArrayList<>();
        JSONArray array;
        try {
            for (int i = 0; i < config.count; i++) {
                array = new JSONArray();
                array.put(new JSONObject("{\n" +
                        "\"id\": \"183639c4-cb37-458e-a8ef-4e130d767ebf" + i + "\",\n" +
                        "\"type\": \"schema\",\n" +
                        "\"detail\": {\n" +
                        "\"id\": \"183639c4-cb37-458e-a8ef-4e130d767ebf" + i + "\",\n" +
                        "\"schema\": \"https://ns.adobe.com/personalization/message/feed-item\",\n" +
                        "\"data\": {\n" +
                        "\"expiryDate\": 1723163897,\n" +
                        "\"meta\": {\n" +
                        "\"feedName\": \"apifeed\",\n" +
                        "\"campaignName\": \"testCampaign\",\n" +
                        "\"surface\": \"mobileapp://com.adobe.sampleApp/feed/promos\"\n" +
                        "},\n" +
                        "\"content\": {\n" +
                        "\"body\": \"testBody\",\n" +
                        "\"title\": \"testTitle\",\n" +
                        "\"imageUrl\": \"https://someimage" + i + ".png\",\n" +
                        "\"actionTitle\": \"testActionTitle\",\n" +
                        "\"actionUrl\": \"https://someurl.com\",\n" +
                        "},\n" +
                        "\"contentType\": \"application/json\",\n" +
                        "\"publishedDate\": 1691541497\n" +
                        "}\n" +
                        "}\n" +
                        "}"));
                consequences.add(array);
            }

        item.put("schema", "https://ns.adobe.com/personalization/ruleset-item");
        item.put("id", "testItemId");
        messageExecution.put("messageExecutionID", "testExecutionId");
        cjmEvent.put("messageExecution", messageExecution);
        characteristics.put("cjmEvent", cjmEvent);
        scopeDetails.put("scopeDetails", characteristics);
        JSONArray rulesArray = new JSONArray();
            for (int i = 0; i < config.count; i++) {
                rulesArray.put(i, new JSONObject("{ \n" +
                        "    \"condition\": {\n" +
                        "      \"type\": \"group\",\n" +
                        "      \"definition\": {\n" +
                        "        \"logic\": \"and\",\n" +
                        "        \"conditions\": [{\n" +
                        "            \"definition\": {\n" +
                        "              \"key\": \"action\",\n" +
                        "              \"matcher\": \"eq\",\n" +
                        "              \"values\": [\n" +
                        "                \"feed\"\n" +
                        "              ]\n" +
                        "            },\n" +
                        "            \"type\": \"matcher\"\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"matcher\",\n" +
                        "            \"definition\": {\n" +
                        "              \"key\": \"~timestampu\",\n" +
                        "              \"matcher\": \"ge\",\n" +
                        "              \"values\": [\n" +
                        "                1680555536\n" +
                        "              ]\n" +
                        "            }\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"matcher\",\n" +
                        "            \"definition\": {\n" +
                        "              \"key\": \"~timestampu\",\n" +
                        "              \"matcher\": \"le\",\n" +
                        "              \"values\": [\n" +
                        "                1790873200\n" +
                        "              ]\n" +
                        "            }\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"consequences\":" +
                        consequences.get(i) +
                        " }\n"));
            }
        data.put("version", 1);
        data.put("rules", rulesArray);

        item.put("data", data);
        items.add(item);

        } catch (JSONException jsonException) {
            Log.debug("MessagingTestUtils", "generateFeedPayload", "exception occurred when creating feed consequences: %s", jsonException.getLocalizedMessage());
        }

        Map<String, Object> messagePayload = new HashMap<>();

        // scope details modification
        if (!config.isMissingScopeDetails) {
            messagePayload.put("scopeDetails", scopeDetails);
        }

        // scope modification
        if (!config.noValidAppSurfaceInPayload) {
            messagePayload.put("scope", "mobileapp://mockPackageName");
        }

        if (config.nonMatchingAppSurfaceInPayload) {
            messagePayload.put("scope", "mobileapp://invalidId");
        }

        if (config.isMissingScope) {
            messagePayload.remove("scope");
        }

        messagePayload.put("items", items);
        messagePayload.put("id", "testResponseId");
        List<Map<String, Object>> payload = new ArrayList<>();
        if (!config.hasEmptyPayload) {
            payload.add(messagePayload);
        }
        return payload;
    }

    static String convertPropositionsToString(List<Proposition> propositions) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(propositions);
            objectOutputStream.defaultWriteObject();
            return objectOutputStream.toString();
        } catch (Exception e) {
            Log.debug(LOG_TAG, LOG_TAG, "Exception occurred while converting payloads to string: %s", e.getMessage());
            return "";
        }
    }

    static String convertPropositionPayloadsToString(List<PropositionPayload> propositionPayloads) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(propositionPayloads);
            objectOutputStream.defaultWriteObject();
            return objectOutputStream.toString();
        } catch (Exception e) {
            Log.debug(LOG_TAG, LOG_TAG, "Exception occurred while converting payloads to string: %s", e.getMessage());
            return "";
        }
    }

    /* JSON conversion helpers */

    /**
     * Converts provided {@link org.json.JSONObject} into {@link java.util.Map} for any number of levels, which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonObject to be converted
     * @return {@link java.util.Map} containing the elements from the provided json, null if {@code jsonObject} is null
     */
    static Map<String, Object> toMap(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            Log.debug(LOG_TAG, LOG_TAG, "toMap - will not convert to map, the passed in json is null.");
            return null;
        }

        Map<String, Object> jsonAsMap = new HashMap<>();
        Iterator<String> keysIterator = jsonObject.keys();

        if (keysIterator == null) return null;

        while (keysIterator.hasNext()) {
            String nextKey = keysIterator.next();
            jsonAsMap.put(nextKey, fromJson(jsonObject.get(nextKey)));
        }

        return jsonAsMap;
    }

    /**
     * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonArray to be converted
     * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
     */
    static List<Object> toList(final JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            Log.debug(LOG_TAG, LOG_TAG, "toList - will not convert to list, the passed in json array is null.");
            return null;
        }

        List<Object> jsonArrayAsList = new ArrayList<>();
        int size = jsonArray.length();

        for (int i = 0; i < size; i++) {
            jsonArrayAsList.add(fromJson(jsonArray.get(i)));
        }

        return jsonArrayAsList;
    }

    /**
     * Converts provided {@link Object} to a {@link JSONObject} or {@link JSONArray}.
     *
     * @param object to be converted to jSON
     * @return {@link Object} containing a json object or json array
     */
    static Object toJSON(final Object object) throws JSONException {
        if (object instanceof HashMap) {
            JSONObject jsonObject = new JSONObject();
            final Map map = (HashMap) object;
            for (final Object key : map.keySet()) {
                jsonObject.put(key.toString(), toJSON(map.get(key)));
            }
            return jsonObject;
        } else if (object instanceof Iterable) {
            JSONArray jsonArray = new JSONArray();
            final Iterator iterator = ((Iterable<?>) object).iterator();
            while (iterator.hasNext()) {
                jsonArray.put(toJSON(iterator.next()));
            }
            return jsonArray;
        } else {
            return object;
        }
    }

    /**
     * Converts provided {@link JSONObject} to a {@link Map} or {@link JSONArray} into a {@link List}.
     *
     * @param json to be converted
     * @return {@link Object} converted from the provided json object.
     */
    private static Object fromJson(final Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }

    static String streamToString(final InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] data = new byte[STREAM_READ_BUFFER_SIZE];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toString(CHARSET_UTF_8);
        } catch (final IOException ex) {
            Log.debug(LOG_TAG, LOG_TAG, "Unable to convert InputStream to String, %s", ex.getLocalizedMessage());
            return null;
        }
    }

    static List<RuleConsequence> createFeedConsequenceList(int size) {
        List<RuleConsequence> feedConsequences = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            try {
                JSONObject feedDetails = new JSONObject("{\n" +
                        "\"id\": \"183639c4-cb37-458e-a8ef-4e130d767ebf" + i + "\",\n" +
                        "\"schema\": \"https://ns.adobe.com/personalization/message/feed-item\",\n" +
                        "\"data\": {\n" +
                        "\"expiryDate\": 1723163897,\n" +
                        "\"meta\": {\n" +
                        "\"feedName\": \"apifeed\",\n" +
                        "\"campaignName\": \"testCampaign\",\n" +
                        "\"surface\": \"mobileapp://com.adobe.sampleApp/feed/promos\"\n" +
                        "},\n" +
                        "\"content\": {\n" +
                        "\"body\": \"testBody\",\n" +
                        "\"title\": \"testTitle\",\n" +
                        "\"imageUrl\": \"https://someimage" + i + ".png\",\n" +
                        "\"actionTitle\": \"testActionTitle\",\n" +
                        "\"actionUrl\": \"https://someurl.com\",\n" +
                        "},\n" +
                        "\"contentType\": \"application/json\",\n" +
                        "\"publishedDate\": 1691541497\n" +
                        "}\n" +
                        "}");
                Map<String, Object> detail = JSONUtils.toMap(feedDetails);
                RuleConsequence feedConsequence = new RuleConsequence("183639c4-cb37-458e-a8ef-4e130d767ebf" + i, MessagingConstants.MessageFeedValues.SCHEMA, detail);
                feedConsequences.add(feedConsequence);
            } catch (JSONException jsonException) {
                fail(jsonException.getMessage());
            }
        }
        return feedConsequences;
    }

    static List<PropositionItem> createMessagingPropositionItemList(int size) {
        List<RuleConsequence> consequences = createFeedConsequenceList(size);
        List<PropositionItem> propositionItemList = new ArrayList<>();
        for (RuleConsequence consequence : consequences) {
            propositionItemList.add(PropositionItem.fromRuleConsequence(consequence));
        }
        return propositionItemList;
    }

}