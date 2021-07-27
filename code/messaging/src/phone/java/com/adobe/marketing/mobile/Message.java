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

import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

public class Message extends MessageDelegate {
    private final static String SELF_TAG = "Message";
    private UIService.FullscreenMessage fullscreenMessage;
    private UIService.FullscreenMessageDelegate customDelegate;

    /**
     * Constructor.
     * <p>
     * Every {@link Message} requires a {@link #messageId}, and must be of type "cjmiam".
     * <p>
     * The consequence {@code Map} for a {@code Message} is required to have valid values for the following fields:
     * <ul>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@link String} containing the message ID</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Variant>} containing details of the Message to be displayed</li>
     * </ul>
     *
     * @param parent {@link MessagingInternal} instance that created this Message.
     * @param consequence {@link Map} containing a {@code Message} defining payload
     * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
     */
    public Message(final MessagingInternal parent, final Map consequence) throws MessageRequiredFieldMissingException {
        this.messagingInternal = parent;
        this.messageId = (String) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID);

        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, "%s - Invalid consequence. Required field \"id\" is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        final String consequenceType = (String) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE);

        if (!MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE.equals(consequenceType)) {
            Log.debug(LOG_TAG, "%s - Invalid consequence. Required field \"type\" is (%s) should be of type (cjmiam).", SELF_TAG,
                    consequenceType);
            throw new MessageRequiredFieldMissingException("Required field: \"type\" is not equal to \"cjmiam\".");
        }

        this.details = (Map) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
        if (details == null || details.isEmpty()) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "%s - Invalid consequence. Required field \"detail\" is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
        }

        final String template = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE);
        if (StringUtils.isNullOrEmpty(template) || !template.equals(MessagingConstants.EventDataKeys.MessageTemplate.FULLSCREEN)) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "%s - Unable to create an in-app message due to a missing or unsupported message template: %s.", SELF_TAG, template);
            throw new MessageRequiredFieldMissingException("Required field: \"template\" is null, empty, or contains an unsupported template type");
        }

        final String html = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "%s - Unable to create an in-app message, the html payload is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        this.customDelegate = MobileCore.getMessagingDelegate();

        if (customDelegate != null) {
            this.fullscreenMessage = MessagingUtils.getUIService().createFullscreenMessage(html, customDelegate, false, this);
        } else {
            this.fullscreenMessage = MessagingUtils.getUIService().createFullscreenMessage(html, this, false, this);
        }
    }

    // ui management
    public void show() {
        if (fullscreenMessage != null) {
            if (autoTrack) {
                track("triggered");
            }
            fullscreenMessage.show();
        }
    }

    public void dismiss() {
        if (fullscreenMessage != null) {
            if (autoTrack) {
                track("dismissed");
            }
            fullscreenMessage.dismiss();
        }
    }
}
