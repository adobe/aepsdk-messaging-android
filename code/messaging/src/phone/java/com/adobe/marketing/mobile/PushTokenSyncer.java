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

        if (dccsUrl == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, dccs url is null.");
            return;
        }

        if (token == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, token is null.");
            return;
        }

        if (ecid == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, ecid is null.");
            return;
        }

        byte[] payload = ("{\n" +
                "\t\"header\": {\n" +
                "\t\t\"imsOrgId\": \"745F37C35E4B776E0A49421B@AdobeOrg\",\n" +
                "\t\t\"source\": {\n" +
                "\t\t  \"name\": \"mobile\"\n" +
                "\t\t},\n" +
                "\t\t\"datasetId\": \"5f5fb49f6b169219511e3988\"\n" + // Messaging SDK dataset in Stage Sandbox for E2E testing
                "\t\t},\n" +
                "\t\t\"body\": {\n" +
                "\t\t\"xdmEntity\": {\n" +
                "\t\t\t\"_cjmstage\": {\n" +
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
