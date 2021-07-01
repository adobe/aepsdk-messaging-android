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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

final class Message extends MessagingDelegate {
    MessagingInternal parent;
    String messageId;
    UIService.UIFullScreenMessage fullscreenMessage;
    MessagingDelegate customDelegate;
    Map<String, Object> experienceInfo;

    /**
     * Constructor.
     * <p>
     * Every {@link Message} requires a {@link #messageId}, and must be of type MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_CJM_VALUE}.
     * <p>
     * The {@code consequence} parameter for a {@code InAppNotification} is required to have valid values for the following fields:
     * <ul>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@link String} containing the message ID</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Variant>} containing details of the InAppNotification</li>
     * </ul>
     *
     * @param parent {@link MessagingInternal} instance that is the parent of the {@code InAppNotificationHandler}
     * @param consequence {@link Map} containing a {@code Message} defining payload
     * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
     */
    Message(final MessagingInternal parent, final Map consequence) throws MessageRequiredFieldMissingException {
        this.parent = parent;
        this.messageId = (String) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID);

        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, "Invalid consequence. Required field \"id\" is null or empty.");
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        final String consequenceType = (String) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE);

        if (!MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE.equals(consequenceType)) {
            Log.debug(LOG_TAG, "Invalid consequence. Required field \"type\" is (%s) should be of type (cjmiam).",
                    consequenceType);
            throw new MessageRequiredFieldMissingException("Required field: \"type\" is not equal to \"cjmiam\".");
        }

        final Map details = (Map) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
        if (details == null || details.isEmpty()) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "Invalid consequence. Required field \"detail\" is null or empty.");
            throw new MessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
        }

        final String template = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE);
        if (StringUtils.isNullOrEmpty(template) || !template.equals(MessagingConstants.EventDataKeys.MessageTemplate.FULLSCREEN)) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message due to a missing or unsupported message template: %s.", template);
            throw new MessageRequiredFieldMissingException("Required field: \"template\" is null, empty, or contains an unsupported template type");
        }

        final String html = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message, the html payload is null or empty.");
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        customDelegate = (MessagingDelegate) MobileCore.getMessagingDelegate();

        if (customDelegate != null) {
            this.fullscreenMessage = getUIService().createFullscreenMessage(html, customDelegate);
        } else {
            this.fullscreenMessage = getUIService().createFullscreenMessage(html, this);
        }
    }

    /**
     * Shows the in-app notification
     */
    void showMessage() {
        if (customDelegate != null) {
            if(!customDelegate.getShowMessageStatus()) {
                Log.debug(MessagingConstants.LOG_TAG,
                        "In-app messages are suppressed, will not show the message.");
                return;
            }
        }
        fullscreenMessage.show();
    }

    /**
     * Returns the {@code UIService} instance.
     *
     * @return {@link UIService} or null if {@link PlatformServices} are unavailable
     */
    private UIService getUIService() {
        final PlatformServices platformServices = MobileCore.getCore().eventHub.getPlatformServices();

        if (platformServices == null) {
            Log.debug(LOG_TAG,
                    "getPlatformServices - Platform services are not available.");
            return null;
        }

        return platformServices.getUIService();
    }
}