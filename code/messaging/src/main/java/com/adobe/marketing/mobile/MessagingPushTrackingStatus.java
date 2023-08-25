/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

public enum MessagingPushTrackingStatus {
    TRACKING_INITIATED(0, "Tracking initiated"),
    NO_DATASET_CONFIGURED(1, "No dataset configured"),
    NO_TRACKING_DATA(2, "Missing tracking data in the intent"),
    INVALID_INTENT(3, "Provided intent for tracking is invalid"),
    INVALID_MESSAGE_ID(4, "Provided MessageId for tracking is empty/null"),
    UNKNOWN_ERROR(5, "Unknown error");

    MessagingPushTrackingStatus(final int value, final String description) {
        this.value = value;
        this.description = description;
    }
    final int value;
    final String description;

    public String getDescription() {
        return description;
    }

    public int getValue() {
        return value;
    }

    public static MessagingPushTrackingStatus fromInt(final int value) {
        for (MessagingPushTrackingStatus b : MessagingPushTrackingStatus.values()) {
            if (b.value == (value)) {
                return b;
            }
        }

        return UNKNOWN_ERROR;
    }
}
