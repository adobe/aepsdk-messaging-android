/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import androidx.annotation.NonNull;

public enum MessagingEdgeEventType {
    PUSH_APPLICATION_OPENED(4),
    PUSH_CUSTOM_ACTION(5),
    DISMISS(6),
    INTERACT(7),
    TRIGGER(8),
    DISPLAY(9),
    DISQUALIFY(10),
    SUPPRESS_DISPLAY(11);

    static final String PROPOSITION_EVENT_TYPE_DISMISS = "dismiss";
    static final String PROPOSITION_EVENT_TYPE_INTERACT = "interact";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER = "trigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY = "display";
    static final String PROPOSITION_EVENT_TYPE_DISQUALIFY = "disqualify";
    static final String PROPOSITION_EVENT_TYPE_SUPPRESS_DISPLAY = "suppressDisplay";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED =
            "pushTracking.applicationOpened";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION =
            "pushTracking.customAction";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER_STRING = "decisioning.propositionTrigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY_STRING = "decisioning.propositionDisplay";
    static final String PROPOSITION_EVENT_TYPE_INTERACT_STRING = "decisioning.propositionInteract";
    static final String PROPOSITION_EVENT_TYPE_DISMISS_STRING = "decisioning.propositionDismiss";
    static final String PROPOSITION_EVENT_TYPE_DISQUALIFY_STRING =
            "decisioning.propositionDisqualify";
    static final String PROPOSITION_EVENT_TYPE_SUPPRESS_DISPLAY_STRING =
            "decisioning.propositionSuppressDisplay";

    final int value;

    MessagingEdgeEventType(final int value) {
        this.value = value;
    }

    /**
     * @deprecated This method will be removed in future versions.
     */
    @Deprecated
    public int getValue() {
        return value;
    }

    public String getPropositionEventType() {
        switch (this) {
            case DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS;
            case INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT;
            case TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER;
            case DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY;
            case DISQUALIFY:
                return PROPOSITION_EVENT_TYPE_DISQUALIFY;
            case SUPPRESS_DISPLAY:
                return PROPOSITION_EVENT_TYPE_SUPPRESS_DISPLAY;
            default:
                return "";
        }
    }

    @NonNull @Override
    public String toString() {
        switch (this) {
            case DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS_STRING;
            case INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT_STRING;
            case TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER_STRING;
            case DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY_STRING;
            case DISQUALIFY:
                return PROPOSITION_EVENT_TYPE_DISQUALIFY_STRING;
            case SUPPRESS_DISPLAY:
                return PROPOSITION_EVENT_TYPE_SUPPRESS_DISPLAY_STRING;
            case PUSH_APPLICATION_OPENED:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED;
            case PUSH_CUSTOM_ACTION:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION;
            default:
                return super.toString();
        }
    }
}
