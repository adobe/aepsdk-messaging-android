package com.adobe.marketing.mobile;

import java.util.Collections;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

// Temp
// This class will be deleted later once we have AEP Platform SDK support for profile data.
class PushTokenSyncer {

    private final NetworkService networkService;

    PushTokenSyncer(final NetworkService networkService) {
        this.networkService = networkService;
    }

    void syncPushToken(final String token, final String ecid, final String dccsUrl) {

        byte[] payload = ("{\n" +
                "\t\"header\": {\n" +
                "\t\t\"schemaRef\": {\n" +
                "\t\t  \"id\": \"https://ns.adobe.com/acopprod3/schemas/393fe4b3364b0856c909a6476260d45f10b360b058e93caa\",\n" + // Messaging SDK Schema in Stage Sandbox for E2E testing
                "\t\t  \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t},\n" +
                "\t\t\"imsOrgId\": \"FAF554945B90342F0A495E2C@AdobeOrg\",\n" +
                "\t\t\"source\": {\n" +
                "\t\t  \"name\": \"mobile\"\n" +
                "\t\t},\n" +
                "\t\t\"datasetId\": \"5ef3e83e6919231915e11ca1\"\n" + // Messaging SDK dataset in Stage Sandbox for E2E testing
                "\t\t},\n" +
                "\t\t\"body\": {\n" +
                "\t\t\"xdmMeta\": {\n" +
                "\t\t  \"schemaRef\": {\n" +
                "\t\t    \"id\": \"https://ns.adobe.com/acopprod3/schemas/393fe4b3364b0856c909a6476260d45f10b360b058e93caa\",\n" +
                "\t\t    \"contentType\": \"application/vnd.adobe.xed-full+json;version=1.28\"\n" +
                "\t\t  }\n" +
                "\t\t},\n" +
                "\t\t\"xdmEntity\": {\n" +
                "\t\t\t\"_acopprod3\": {\n" +
                "\t\t  \t\t\"ECID\": \"" + ecid + "\"\n" +
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

        final NetworkService.HttpConnection connection = networkService.connectUrl(dccsUrl, NetworkService.HttpCommand.POST, payload, Collections.singletonMap("Content-Type", "application/json"), 10, 10);
        if (connection.getResponseCode() == 200) {
            Log.debug(LOG_TAG, "Successfully synced push token %s", token);
        } else {
            Log.debug(LOG_TAG, "Error in syncing push token %s: \n ERROR: %s", token, connection.getResponseMessage());
        }
    }
}
