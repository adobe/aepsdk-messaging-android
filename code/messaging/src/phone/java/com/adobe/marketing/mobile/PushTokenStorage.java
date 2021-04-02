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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Temp
//This class will be deleted and all the code will be moved to platform service SDK.
class PushTokenStorage {

    private static final String PREFERENCE_NAME = "AdobeMobile_ExperienceMessage";
    private static final String KEY = "pushIdentifier";

    private final LocalStorageService localStorageService;

    PushTokenStorage(final LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    void storeToken(final String pushToken) {
        final LocalStorageService.DataStore dataStore = localStorageService.getDataStore(PREFERENCE_NAME);
        dataStore.setString(KEY, getShaHash(pushToken));
    }

    void removeToken() {
        LocalStorageService.DataStore dataStore = localStorageService.getDataStore(PREFERENCE_NAME);
        dataStore.remove(KEY);
    }

    private static String getShaHash(final String pushToken) {
        if (pushToken != null) {
            try {
                final MessageDigest messageDigest = MessageDigest.getInstance("sha-256");
                final byte[] hashedBytes = messageDigest.digest(pushToken.getBytes());
                return new String(hashedBytes);
            } catch (NoSuchAlgorithmException e) {
                Log.warning(MessagingConstant.LOG_TAG, "Error in creating sha hash for push token.");
            }
        }
        return pushToken;
    }
}
