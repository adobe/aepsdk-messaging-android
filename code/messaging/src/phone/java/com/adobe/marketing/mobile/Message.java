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

import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.MobileParametersKeys;

public class Message extends MessageDelegate {
    private final static String SELF_TAG = "Message";
    private AEPMessage aepMessage;
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
    public Message(final MessagingInternal parent, final Map consequence, final Map<String, Object> rawMessageSettings) throws MessageRequiredFieldMissingException {
        this.messagingInternal = parent;

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

        final Map<String, Object> xdmMap = (HashMap) details.get(MessagingConstants.TrackingKeys._XDM);
        final Map<String, Object> mixinMap = (HashMap) xdmMap.get(MessagingConstants.TrackingKeys.MIXINS);
        final Map<String, Object> experienceMap = (HashMap) mixinMap.get(MessagingConstants.TrackingKeys.EXPERIENCE);
        final Map<String, Object> cjmMap = (HashMap) experienceMap.get(MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT);
        final Map<String, Object> messageExecutionMap = (HashMap) cjmMap.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION);
        this.messageId = (String) messageExecutionMap.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID);

        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, "%s - Invalid consequence. Required field \"id\" is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        final String html = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "%s - Unable to create an in-app message, the html payload is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        // message customization poc
        final UIService.MessageSettings settings;
        AEPMessageSettings.Builder messageSettingsBuilder = new AEPMessageSettings.Builder(this);
        if(rawMessageSettings != null && !rawMessageSettings.isEmpty()) {
            settings = addMessageSettings(messageSettingsBuilder, rawMessageSettings);
        } else {
            settings = messageSettingsBuilder.build();
        }

        this.customDelegate = MobileCore.getFullscreenMessageDelegate();
        if (customDelegate != null) {
            this.aepMessage = (AEPMessage) MessagingUtils.getUIService().createFullscreenMessage(html, customDelegate, false, settings);
        } else {
            this.aepMessage = (AEPMessage) MessagingUtils.getUIService().createFullscreenMessage(html, this, false, settings);
        }
    }

    // ui management
    public void show() {
        if (aepMessage != null) {
            if (autoTrack) {
                track("triggered");
            }
            aepMessage.show();
        }
    }

    public void dismiss() {
        if (aepMessage != null) {
            if (autoTrack) {
                track("dismissed");
            }

            aepMessage.dismiss();
        }
    }

    // message customization poc
    /**
     * Sample Message payload:
     *      "messageSetting": {
     *         "schemaVersion": "1.0",
     *          "width": "80",
     *          "height": "50",
     *          "verticalAlign": "center",
     *          "verticalInset": "0",
     *          "horizontalAlign": "center",
     *          "horizontalInset": "0",
     *          "uiTakeover": true,
     *          "displayAnimation": "top",
     *          "dismissAnimation": "top",
     *          "backdropColor": "AA00EE",
     *          "backdropOpacity": 0.2,
     *          "gestures": {
     *              "swipeUp": "adbinapp://dismiss",
     *              "swipeDown": "adbinapp://dismiss",
     *              "swipeLeft": "adbinapp://dismiss?interaction=negative",
     *              "swipeRight": "adbinapp://dismiss?interaction=positive",
     *              "tapBackground": "adbinapp://dismiss"
     *          }
     *      }
     */
    private UIService.MessageSettings addMessageSettings(AEPMessageSettings.Builder builder, final Map<String, Object> rawSettings) {
        int width, height, verticalInset, horizontalInset;
        String backdropColor;
        float backdropOpacity;
        float cornerRadius;
        boolean uiTakeover;
        UIService.MessageAlignment verticalAlign, horizontalAlign;
        UIService.MessageAnimation displayAnimation, dismissAnimation;
        Map<UIService.MessageGesture, String> gestureStringMap = new HashMap<>();

        if (rawSettings.get(MobileParametersKeys.WIDTH) == null) {
            width = 100;
        } else {
            width = Integer.parseInt(rawSettings.get(MobileParametersKeys.WIDTH).toString());
        }

        if (rawSettings.get(MobileParametersKeys.HEIGHT) == null) {
            height = 100;
        } else {
            height = Integer.parseInt(rawSettings.get(MobileParametersKeys.HEIGHT).toString());
        }

        if (rawSettings.get(MobileParametersKeys.VERTICAL_ALIGN) == null) {
            verticalAlign = UIService.MessageAlignment.CENTER;
        } else {
            verticalAlign = UIService.MessageAlignment.valueOf(((String) rawSettings.get(MobileParametersKeys.VERTICAL_ALIGN)).toUpperCase());
        }

        if (rawSettings.get(MobileParametersKeys.VERTICAL_INSET) == null) {
            verticalInset = 0;
        } else {
            verticalInset = Integer.parseInt(rawSettings.get(MobileParametersKeys.VERTICAL_INSET).toString());
        }

        if (rawSettings.get(MobileParametersKeys.HORIZONTAL_ALIGN) == null) {
            horizontalAlign = UIService.MessageAlignment.CENTER;
        } else {
            horizontalAlign = UIService.MessageAlignment.valueOf(((String) rawSettings.get(MobileParametersKeys.HORIZONTAL_ALIGN)).toUpperCase());
        }

        if (rawSettings.get(MobileParametersKeys.HORIZONTAL_INSET) == null) {
            horizontalInset = 0;
        } else {
            horizontalInset = Integer.parseInt(rawSettings.get(MobileParametersKeys.HORIZONTAL_INSET).toString());
        }

        if (rawSettings.get(MobileParametersKeys.DISPLAY_ANIMATION) == null) {
            displayAnimation = UIService.MessageAnimation.NONE;
        } else {
            displayAnimation = UIService.MessageAnimation.valueOf(((String) rawSettings.get(MobileParametersKeys.DISPLAY_ANIMATION)).toUpperCase());
        }

        if (rawSettings.get(MobileParametersKeys.DISMISS_ANIMATION) == null) {
            dismissAnimation = UIService.MessageAnimation.NONE;
        } else {
            dismissAnimation = UIService.MessageAnimation.valueOf(((String) rawSettings.get(MobileParametersKeys.DISMISS_ANIMATION)).toUpperCase());
        }

        if (rawSettings.get(MobileParametersKeys.BACKDROP_COLOR) == null) {
            backdropColor = "#FFFFFF";
        } else {
            backdropColor = (String) rawSettings.get(MobileParametersKeys.BACKDROP_COLOR);
        }

        if (rawSettings.get(MobileParametersKeys.BACKDROP_OPACITY) == null) {
            backdropOpacity = 0.0f;
        } else {
            final double opacity = ((Double) rawSettings.get(MobileParametersKeys.BACKDROP_OPACITY));
            backdropOpacity = (float) opacity;
        }

        if (rawSettings.get(MobileParametersKeys.CORNER_RADIUS) == null) {
            cornerRadius = 0.0f;
        } else {
            final int radius = ((Integer) rawSettings.get(MobileParametersKeys.CORNER_RADIUS));
            cornerRadius = (float) radius;
        }

        if (rawSettings.get(MobileParametersKeys.UI_TAKEOVER) == null) {
            uiTakeover = true;
        } else {
            uiTakeover = Boolean.getBoolean(rawSettings.get(MobileParametersKeys.UI_TAKEOVER).toString());
        }

        // we need to convert key strings present in the gestures map to MessageGesture enum keys
        final Map<String, String> stringMap = (Map<String, String>) rawSettings.get(MobileParametersKeys.GESTURES);
        if (stringMap != null && !stringMap.isEmpty()) {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                final UIService.MessageGesture gesture = UIService.MessageGesture.get(entry.getKey());
                gestureStringMap.put(gesture, entry.getValue());
            }
        }
        
        return builder.setWidth(width)
                .setHeight(height)
                .setVerticalInset(verticalInset)
                .setHorizontalInset(horizontalInset)
                .setVerticalAlign(verticalAlign)
                .setHorizontalAlign(horizontalAlign)
                .setDisplayAnimation(displayAnimation)
                .setDismissAnimation(dismissAnimation)
                .setBackdropColor(backdropColor)
                .setBackdropOpacity(backdropOpacity)
                .setCornerRadius(cornerRadius)
                .setUiTakeover(uiTakeover)
                .setGestures(gestureStringMap)
                .build();
    }
}
