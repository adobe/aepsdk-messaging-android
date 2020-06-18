package com.adobe.marketing.mobile;

import java.util.Collections;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

//This class will be deleted later once we have AEP Platform SDK support for profile data.
public class PushTokenSyncer {

    private final NetworkService networkService;
    private static final String DATA_INGESTION_URL = "https://dcs.adobedc.net/collection/a4a946939737c99b4d09d43c570266d42d2ef62546aa84fe894bac71d8bf98f1";

    public PushTokenSyncer(final NetworkService networkService) {
        this.networkService = networkService;
    }

    void syncPushToken(final String token, final String ecid) {

        byte[] payload = ("{\n" +
                "\t\"header\": {\n" +
                "\t\t\"schemaRef\": {\n" +
                "\t\t  \"id\": \"https://ns.adobe.com/acopprod1/schemas/28b3d7114b095f6aff015a71a6d6667a5f466fc7c9826000\",\n" +
                "\t\t  \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t},\n" +
                "\t\t\"imsOrgId\": \"3E2A28175B8ED3720A495E23@AdobeOrg\",\n" +
                "\t\t\"source\": {\n" +
                "\t\t  \"name\": \"mobile\"\n" +
                "\t\t},\n" +
                "\t\t\"datasetId\": \"5ee2aa2f239d341915e991ea\"\n" +
                "\t\t},\n" +
                "\t\t\"body\": {\n" +
                "\t\t\"xdmMeta\": {\n" +
                "\t\t  \"schemaRef\": {\n" +
                "\t\t    \"id\": \"https://ns.adobe.com/acopprod1/schemas/28b3d7114b095f6aff015a71a6d6667a5f466fc7c9826000\",\n" +
                "\t\t    \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t  }\n" +
                "\t\t},\n" +
                "\t\t\"xdmEntity\": {\n" +
                "\t\t\t\"_acopprod1\": {\n" +
                "\t\t  \t\t\"primaryid\": \"" + ecid + "\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"pushNotificationDetails\": [\n" +
                "\t\t      \t{\n" +
                "\t\t      \t\t\"appID\": \"" + App.getApplication().getPackageName() + "\",\n" +
                "\t\t   \t\t\t\"platform\": \"fcm\",\n" +
                "\t\t    \t\t\"token\": \"" + token + "\",\n" +
                "\t\t    \t\t\"blacklisted\": false,\n" +
                "\t\t    \t\t\"identiy\": {\n" +
                "\t\t    \t\t\t\"namespace\": {\n" +
                "\t\t    \t\t\t\t\"code\": \"ECID\"\n" +
                "\t\t      \t\t\t},\n" +
                "\t\t      \t\t\t\"xid\": \"" + ecid + "\"\n" +
                "\t\t      \t\t}\n" +
                "\t\t      \t}\n" +
                "\t\t    ]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}").getBytes();

        NetworkService.HttpConnection connection = networkService.connectUrl(DATA_INGESTION_URL, NetworkService.HttpCommand.POST, payload, Collections.singletonMap("Content-Type", "application/json"), 10, 10);
        if (connection.getResponseCode() == 200) {
            Log.debug(LOG_TAG, "Successfully synced push token %s", token);
        } else {
            Log.debug(LOG_TAG, "Error in syncing push token %s: \n ERROR: %s", token, connection.getResponseMessage());
        }
    }
}
