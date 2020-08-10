package com.adobe.marketing.mobile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//This class will be deleted and all the code will be moved to platform service SDK.
public class PushTokenStorage {

    private static final String PREFERENCE_NAME = "AdobeMobile_ExperienceMessage";
    private static final String KEY = "pushIdentifier";

    private LocalStorageService localStorageService;

    public PushTokenStorage(final LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    void storeToken(final String pushToken){
        final LocalStorageService.DataStore dataStore = localStorageService.getDataStore(PREFERENCE_NAME);
        dataStore.setString(KEY, getShaHash(pushToken));
    }

    void removeToken() {
        LocalStorageService.DataStore dataStore = localStorageService.getDataStore(PREFERENCE_NAME);
        dataStore.remove(KEY);
    }

    private static String getShaHash(final String pushToken) {
        if(pushToken != null) {
            try {
                final MessageDigest messageDigest = MessageDigest.getInstance("sha-256");
                final byte[] hashedBytes = messageDigest.digest(pushToken.getBytes());
                return new String(hashedBytes);
            } catch (NoSuchAlgorithmException e) {
                Log.error(MessagingConstant.LOG_TAG, "Error in creating sha hash for push token.");
            }
        }
        return pushToken;
    }
}
