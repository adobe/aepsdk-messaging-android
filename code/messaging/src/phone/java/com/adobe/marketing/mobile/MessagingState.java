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
