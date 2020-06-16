package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

//This class will be deleted later once we have AEP Platform services SDK.
public class PushTokenSyncer {

    private final NetworkService networkService;
    private final JsonUtilityService jsonUtilityService;
    private static final String DATA_INGESTION_URL = "https://dcs.adobedc.net/collection/a4a946939737c99b4d09d43c570266d42d2ef62546aa84fe894bac71d8bf98f1";

    public PushTokenSyncer(final NetworkService networkService, final JsonUtilityService jsonUtilityService) {
        this.networkService = networkService;
        this.jsonUtilityService = jsonUtilityService;
    }

    void syncPushToken(final String token, final String ecid) {

        byte[] payload = ("{\n" +
                "  \"header\": {\n" +
                "    \"schemaRef\": {\n" +
                "      \"id\": \"https://ns.adobe.com/acopprod1/schemas/28b3d7114b095f6aff015a71a6d6667a5f466fc7c9826000\",\n" +
                "      \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "    },\n" +
                "    \"imsOrgId\": \"3E2A28175B8ED3720A495E23@AdobeOrg\",\n" +
                "    \"source\": {\n" +
                "      \"name\": \"mobile\"\n" +
                "    },\n" +
                "    \"datasetId\": \"5ee2aa2f239d341915e991ea\"\n" +
                "  },\n" +
                "  \"body\": {\n" +
                "    \"xdmMeta\": {\n" +
                "      \"schemaRef\": {\n" +
                "        \"id\": \"https://ns.adobe.com/acopprod1/schemas/28b3d7114b095f6aff015a71a6d6667a5f466fc7c9826000\",\n" +
                "        \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"xdm:pushNotificationDetails\": [\n" +
                "      \t{\n" +
                "      \t\t\"xdm:appID\": \"com.adobe.marketing.mobile.messagingsampleapp\",\n" +
                "   \t\t\t\"xdm:platform\": \"apns\",\n" +
                "    \t\t\"xdm:token\": \"" + token + "\",\n" +
                "    \t\t\"xdm:blacklisted\": false,\n" +
                "    \t\t\"xdm:identiy\": {\n" +
                "    \t\t\t\"xdm:namespace\": {\n" +
                "    \t\t\t\t\"xdm:code\": \"ECID\"\n" +
                "      \t\t\t},\n" +
                "      \t\t\t\"xdm:xid\": \"" + ecid + "\"\n" +
                "      \t\t}\n" +
                "      \t}\n" +
                "    ],\n" +
                "    \"xdmEntity\": {\n" +
                "    \t\"_acopprod1\": {\n" +
                "      \t\t\"primaryid\": \"43363765445157027836050171721132739889\"\n" +
                "    \t}\n" +
                "    }\n" +
                "  }\n" +
                "}").getBytes();

        NetworkService.HttpConnection connection = networkService.connectUrl(DATA_INGESTION_URL, NetworkService.HttpCommand.POST, payload, null, 10, 10);
        if (connection.getResponseCode() == 200) {
            Log.debug(LOG_TAG, "Successfully synced push token %s", token);
        } else {
            Log.debug(LOG_TAG, "Error in syncing push token %s: \n ERROR: %s", token, connection.getResponseMessage());
        }
    }
}
