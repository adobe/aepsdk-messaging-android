package com.adobe.marketing.mobile;

/**
 * MessagingState is used to store configuration and identity information
 */
final class MessagingState {

    //Configuration properties
    private MobilePrivacyStatus privacyStatus = MobilePrivacyStatus.OPT_IN;

    // Temp
    // Temporary implementation for dccs hack for collecting push tokens
    private String dccsURL;
    private String experienceCloudOrg;

    //Identity properties.
    private String ecid;

    // Messaging properties
    private String profileDatasetId;
    private String experienceEventDatasetId;


    void setState(final EventData configState, final EventData identityState) {
        setConfigState(configState);
        setIdentityState(identityState);
    }

    private void setConfigState(final EventData configState) {
        if (configState != null) {
            this.privacyStatus = MobilePrivacyStatus.fromString(configState.optString(MessagingConstant.EventDataKeys.Configuration.GLOBAL_PRIVACY_STATUS, ""));
            this.profileDatasetId = configState.optString(MessagingConstant.EventDataKeys.Configuration.PROFILE_DATASET_ID, "");
            this.experienceEventDatasetId = configState.optString(MessagingConstant.EventDataKeys.Configuration.EXPERIENCE_EVENT_DATASET_ID, "");

            // Temp
            this.dccsURL = configState.optString(MessagingConstant.EventDataKeys.Configuration.DCCS_URL, null);
            this.experienceCloudOrg = configState.optString(MessagingConstant.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORG, null);
        }
    }

    private void setIdentityState(final EventData identityState) {
        if (identityState != null) {
            this.ecid = identityState.optString(MessagingConstant.EventDataKeys.Identity.VISITOR_ID_MID, "");
        }
    }

    MobilePrivacyStatus getPrivacyStatus() {
        return privacyStatus;
    }

    // Temp
    String getDccsURL() {
        return dccsURL;
    }
    String getExperienceCloudOrg() {
        return experienceCloudOrg;
    }

    String getEcid() {
        return ecid;
    }

    String getProfileDatasetId() {
        return profileDatasetId;
    }

    String getExperienceEventDatasetId() {
        return experienceEventDatasetId;
    }
}
