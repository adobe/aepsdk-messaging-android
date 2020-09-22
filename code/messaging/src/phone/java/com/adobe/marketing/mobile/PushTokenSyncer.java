/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

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
                "                    \"blocklisted\": false,\n" +
                "                    \"identity\": {\n" +
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
