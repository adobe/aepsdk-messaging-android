package com.adobe.marketing.mobile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PushTokenStorage {

    private static final String PREFERENCE_NAME = "AdobeMobile_ExperienceMessage";
    private static final String KEY = "pushIdentifier";

    private LocalStorageService localStorageService;

    public PushTokenStorage(final LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    void storeToken(final String pushToken){

        LocalStorageService.DataStore dataStore = localStorageService.getDataStore(PREFERENCE_NAME);
        dataStore.setString(KEY, getShaHash(pushToken));
    }

    private static String getShaHash(final String pushToken){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("sha-256");
            byte[] hashedBytes = messageDigest.digest(pushToken.getBytes());
            return new String(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            Log.debug(MessagingConstant.LOG_TAG, "Error in creating sha hash for push token.");
        }
        return pushToken;
    }
}
