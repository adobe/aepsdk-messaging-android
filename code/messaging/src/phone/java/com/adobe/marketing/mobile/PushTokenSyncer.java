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

    void syncPushToken(final String token, final String ecid, final String dccsUrl, final String experienceCloudOrg, final String profileDatasetId) {

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

        if (experienceCloudOrg == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, experience cloud org is null.");
            return;
        }

        if (profileDatasetId == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, profile dataset id is null.");
            return;
        }

        byte[] payload = ("{\n" +
                "    \"header\" : {\n" +
                "        \"imsOrgId\": \"" + experienceCloudOrg + "\",\n" +
                "        \"source\": {\n" +
                "            \"name\": \"mobile\"\n" +
                "        },\n" +
                "        \"datasetId\": \"" + profileDatasetId +"\"\n" +
                "    },\n" +
                "    \"body\": {\n" +
                "        \"xdmEntity\": {\n" +
                "            \"identityMap\": {\n" +
                "                \"ECID\": [\n" +
                "                    {\n" +
                "                        \"id\" : \"" + ecid +"\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"pushNotificationDetails\": [\n" +
                "                {\n" +
                "                    \"appID\": \"" + App.getApplication().getPackageName() +"\",\n" +
                "                    \"platform\": \"fcm\",\n" +
                "                    \"token\": \"" + token + "\",\n" +
                "                    \"blacklisted\": false,\n" +
                "                    \"blocklisted\": false,\n" +
                "                    \"identiy\": {\n" +
                "                        \"namespace\": {\n" +
                "                            \"code\": \"ECID\"\n" +
                "                        },\n" +
                "                        \"xid\": \"" + ecid + "\"\n" +
                "                    }\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    }\n" +
                "}").getBytes();

        final NetworkService.HttpConnection connection = networkService.connectUrl(dccsUrl, NetworkService.HttpCommand.POST, payload, Collections.singletonMap("Content-Type", "application/json"), 10, 10);
        if (connection.getResponseCode() == 200) {
            Log.debug(LOG_TAG, "Successfully synced push token %s", token);
        } else {
            Log.debug(LOG_TAG, "Error in syncing push token %s: \n ERROR: %s", token, connection.getResponseMessage());
        }
    }
}
