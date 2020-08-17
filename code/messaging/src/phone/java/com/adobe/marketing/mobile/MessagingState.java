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

    //Identity properties.
    private String ecid;


    void setState(final EventData configState, final EventData identityState) {
        setConfigState(configState);
        setIdentityState(identityState);
    }

    void setConfigState(final EventData configState) {
        if (configState != null) {
            this.privacyStatus = MobilePrivacyStatus.fromString(configState.optString(MessagingConstant.EventDataKeys.Configuration.GLOBAL_PRIVACY_STATUS, ""));
            // Temp
            this.dccsURL = configState.optString(MessagingConstant.EventDataKeys.Configuration.DCCS_URL, "https://dcs.adobedc.net/collection/7b0a69f4d9563b792f41c8c7433d37ad5fa58f47ea1719c963c8501bf779e827");
        }
    }

    void setIdentityState(final EventData identityState) {
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

    String getEcid() {
        return ecid;
    }
}
