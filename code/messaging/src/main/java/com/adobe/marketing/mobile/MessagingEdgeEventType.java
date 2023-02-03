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

public enum MessagingEdgeEventType {
    IN_APP_DISMISS(0),
    IN_APP_INTERACT(1),
    IN_APP_TRIGGER(2),
    IN_APP_DISPLAY(3),
    PUSH_APPLICATION_OPENED(4),
    PUSH_CUSTOM_ACTION(5);

    static final String PROPOSITION_EVENT_TYPE_DISMISS = "dismiss";
    static final String PROPOSITION_EVENT_TYPE_INTERACT = "interact";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER = "trigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY = "display";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED = "pushTracking.applicationOpened";
    static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION = "pushTracking.customAction";
    static final String PROPOSITION_EVENT_TYPE_TRIGGER_STRING = "decisioning.propositionTrigger";
    static final String PROPOSITION_EVENT_TYPE_DISPLAY_STRING = "decisioning.propositionDisplay";
    static final String PROPOSITION_EVENT_TYPE_INTERACT_STRING = "decisioning.propositionInteract";
    static final String PROPOSITION_EVENT_TYPE_DISMISS_STRING = "decisioning.propositionDismiss";

    final int value;

    MessagingEdgeEventType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getPropositionEventType() {
        switch (this) {
            case IN_APP_DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS;
            case IN_APP_INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT;
            case IN_APP_TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER;
            case IN_APP_DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY;
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case IN_APP_DISMISS:
                return PROPOSITION_EVENT_TYPE_DISMISS_STRING;
            case IN_APP_INTERACT:
                return PROPOSITION_EVENT_TYPE_INTERACT_STRING;
            case IN_APP_TRIGGER:
                return PROPOSITION_EVENT_TYPE_TRIGGER_STRING;
            case IN_APP_DISPLAY:
                return PROPOSITION_EVENT_TYPE_DISPLAY_STRING;
            case PUSH_APPLICATION_OPENED:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED;
            case PUSH_CUSTOM_ACTION:
                return PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION;
            default:
                return super.toString();
        }
    }
}