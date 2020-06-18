package com.adobe.marketing.mobile;

final class MessagingState {

    //Configuration properties
    private MobilePrivacyStatus privacyStatus = MobilePrivacyStatus.OPT_IN;

    //Identity properties.
    private String ecid;


    void setState(final EventData configState, final EventData identityState) {
        setConfigState(configState);
        setIdentityState(identityState);
    }

    void setConfigState(final EventData configState) {
        if (configState != null) {
            this.privacyStatus = MobilePrivacyStatus.fromString(configState.optString(MessagingConstant.EventDataKeys.Configuration.GLOBAL_PRIVACY_STATUS, ""));
        }
    }

    void setIdentityState(final EventData identityState) {
        if (identityState != null) {
            this.ecid = identityState.optString(MessagingConstant.EventDataKeys.Identity.VISITOR_ID_MID, "");
        }
    }

    public MobilePrivacyStatus getPrivacyStatus() {
        return privacyStatus;
    }

    public String getEcid() {
        return ecid;
    }
}
