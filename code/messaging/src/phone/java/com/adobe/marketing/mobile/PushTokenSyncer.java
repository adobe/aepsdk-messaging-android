package com.adobe.marketing.mobile;

import java.util.Collections;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

//This class will be deleted later once we have AEP Platform SDK support for profile data.
public class PushTokenSyncer {

    private final NetworkService networkService;
    private static final String DATA_INGESTION_URL = "https://dcs.adobedc.net/collection/fbcb86a87bbee3d5ae69bd8789dbd12c9674c9f3a8efcb7c7b30bab9a2966dcf";

    public PushTokenSyncer(final NetworkService networkService) {
        this.networkService = networkService;
    }

    void syncPushToken(final String token, final String ecid) {

        byte[] payload = ("{\n" +
                "\t\"header\": {\n" +
                "\t\t\"schemaRef\": {\n" +
                "\t\t  \"id\": \"https://ns.adobe.com/acopprod3/schemas/60ed18f0f3e30bbb9b7c77bbfb5a4f75a45986e0cdddc033\",\n" +
                "\t\t  \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t},\n" +
                "\t\t\"imsOrgId\": \"FAF554945B90342F0A495E2C@AdobeOrg\",\n" +
                "\t\t\"source\": {\n" +
                "\t\t  \"name\": \"mobile\"\n" +
                "\t\t},\n" +
                "\t\t\"datasetId\": \"5ef3d544791c4a1915e6aa4b\"\n" +
                "\t\t},\n" +
                "\t\t\"body\": {\n" +
                "\t\t\"xdmMeta\": {\n" +
                "\t\t  \"schemaRef\": {\n" +
                "\t\t    \"id\": \"https://ns.adobe.com/acopprod3/schemas/60ed18f0f3e30bbb9b7c77bbfb5a4f75a45986e0cdddc033\",\n" +
                "\t\t    \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t  }\n" +
                "\t\t},\n" +
                "\t\t\"xdmEntity\": {\n" +
                "\t\t\t\"_acopprod3\": {\n" +
                "\t\t  \t\t\"ecid\": \"" + ecid + "\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"pushNotificationDetails\": [\n" +
                "\t\t      \t{\n" +
                "\t\t      \t\t\"appID\": \"" + App.getApplication().getPackageName() + "\",\n" +
                "\t\t   \t\t\t\"platform\": \"fcm\",\n" +
                "\t\t    \t\t\"token\": \"" + token + "\",\n" +
                "\t\t    \t\t\"blacklisted\": false,\n" +
                "\t\t    \t\t\"blocklisted\": false,\n" +
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
