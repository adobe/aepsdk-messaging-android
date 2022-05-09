/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

interface MessagingEventsHandler {

    /**
     * Handles sending push token
     *
     * @param event Identity Request content event which contains the push token.
     */
    void handlePushToken(final Event event);

    /**
     * Handles sending push notification tracking information
     *
     * @param event Messaging Request Content event which contains the push notification tracking information
     */
    void handleTrackingInfo(final Event event);

    /**
     * Processes the Hub Shared State to check if configuration and edge identity states are updated.
     *
     * @param event Hub Shared State event
     */
    void processHubSharedState(final Event event);
}
