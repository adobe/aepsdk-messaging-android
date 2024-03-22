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

package com.adobe.marketing.mobile.messaging;

/** Enum representing the status of push tracking. */
public enum PushTrackingStatus {
    /**
     * This status is set when all the required data for tracking is available and when the tracking
     * is initiated.
     */
    TRACKING_INITIATED(0, "Tracking initiated"),
    /**
     * This status is set when tracking is not initiated because no tracking dataset is configured.
     */
    NO_DATASET_CONFIGURED(1, "No dataset configured"),

    /**
     * This status is set when tracking is not initiated because the intent does not contain
     * tracking data.
     */
    NO_TRACKING_DATA(2, "Missing tracking data in the intent"),
    /** This status is set when tracking is not initiated because the intent is invalid. */
    INVALID_INTENT(3, "Provided intent for tracking is invalid"),
    /** This status is set when tracking is not initiated because the message id is invalid. */
    INVALID_MESSAGE_ID(4, "Provided MessageId for tracking is empty/null"),
    /** This status is set when tracking is not initiated because of an unknown error. */
    UNKNOWN_ERROR(5, "Unknown error");

    PushTrackingStatus(final int value, final String description) {
        this.value = value;
        this.description = description;
    }

    final int value;
    final String description;

    /**
     * @return the string description of {@link PushTrackingStatus}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the enum {@code Integer} value of {@link PushTrackingStatus}
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the {@link PushTrackingStatus} enum for the provided {@code int} value.
     *
     * @param value {@code int} value of the enum
     * @return {@link PushTrackingStatus} enum value
     */
    public static PushTrackingStatus fromInt(final int value) {
        for (final PushTrackingStatus b : PushTrackingStatus.values()) {
            if (b.value == (value)) {
                return b;
            }
        }

        return UNKNOWN_ERROR;
    }
}
