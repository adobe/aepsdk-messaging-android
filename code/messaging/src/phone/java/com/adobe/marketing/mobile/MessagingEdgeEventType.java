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
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.PropositionEventType.DISMISS;
            case IN_APP_INTERACT:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.PropositionEventType.INTERACT;
            case IN_APP_TRIGGER:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.PropositionEventType.TRIGGER;
            case IN_APP_DISPLAY:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.PropositionEventType.DISPLAY;
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case IN_APP_DISMISS:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.DISMISS;
            case IN_APP_INTERACT:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT;
            case IN_APP_TRIGGER:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.TRIGGER;
            case IN_APP_DISPLAY:
                return MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.DISPLAY;
            case PUSH_APPLICATION_OPENED:
                return MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.OPENED;
            case PUSH_CUSTOM_ACTION:
                return MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.CUSTOM_ACTION;
            default:
                return super.toString();
        }
    }
}
