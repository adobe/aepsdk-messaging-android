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
    IN_APP_DISMISS(MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.DISMISS),
    IN_APP_INTERACT(MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT),
    IN_APP_TRIGGER(MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.TRIGGER),
    IN_APP_DISPLAY(MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.DISPLAY),
    PUSH_APPLICATION_OPENED(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.OPENED),
    PUSH_CUSTOM_ACTION(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.INTERACT);

    private final String value;

    MessagingEdgeEventType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
