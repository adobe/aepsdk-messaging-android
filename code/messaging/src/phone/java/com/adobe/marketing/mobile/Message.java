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

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.MobileParametersKeys;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAlignment;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAnimation;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageGesture;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the definition of an in-app message and controls its tracking via Experience Edge events.
 */
public class Message extends MessageDelegate {
    private final static String SELF_TAG = "Message";
    // public properties
    public String id;
    public boolean autoTrack = true;
    public WebView view;
    // private properties
    private final AEPMessage aepMessage;
    private final FullscreenMessageDelegate fullscreenMessageDelegate;
    private final Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();

    /**
     * Constructor.
     * <p>
     * Every {@link Message} requires a {@link #id}, and must be of type "cjmiam".
     * <p>
     * The consequence {@code Map} for a {@code Message} is required to have valid values for the following fields:
     * <ul>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@code String} containing the message ID</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Object>} containing details of the Message to be displayed</li>
     * </ul>
     *
     * @param parent             {@link MessagingInternal} instance that created this Message
     * @param consequence        {@code Map<String, Object>} containing a {@code Message} defining payload
     * @param rawMessageSettings {@code Map<String, Object>} contating the raw message settings found in the "mobileParameters" present in the rule consequence
     * @param assetMap           {@code Map<String, Object>} containing a mapping of a remote image asset URL and it's cached location
     * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
     */
    public Message(final MessagingInternal parent, final Map<String, Object> consequence, final Map<String, Object> rawMessageSettings, final Map<String, String> assetMap) throws MessageRequiredFieldMissingException {
        messagingInternal = parent;

        final String consequenceType = (String) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE);

        if (!MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE.equals(consequenceType)) {
            Log.debug(LOG_TAG, "%s - Invalid consequence (%s). Required field \"type\" is (%s) should be of type (cjmiam).", SELF_TAG,
                    consequence.toString(), consequenceType);
            throw new MessageRequiredFieldMissingException("Required field: \"type\" is not equal to \"cjmiam\".");
        }

        details = (Map<String, Object>) consequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
        if (MessagingUtils.isMapNullOrEmpty(details)) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "%s - Invalid consequence (%s). Required field \"detail\" is null or empty.", SELF_TAG, consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
        }

        id = getMessageId(details);
        if (StringUtils.isNullOrEmpty(id)) {
            Log.debug(LOG_TAG, "%s - Invalid consequence (%s). Required field \"id\" is null or empty.", SELF_TAG, consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        final String html = (String) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "%s - Unable to create an in-app message, the html payload is null or empty.", SELF_TAG);
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        final MessageSettings settings;
        AEPMessageSettings.Builder messageSettingsBuilder = new AEPMessageSettings.Builder(this);
        if (!MessagingUtils.isMapNullOrEmpty(rawMessageSettings)) {
            settings = addMessageSettings(messageSettingsBuilder, rawMessageSettings);
        } else {
            settings = messageSettingsBuilder.build();
        }

        fullscreenMessageDelegate = ServiceProvider.getInstance().getMessageDelegate() != null ? ServiceProvider.getInstance().getMessageDelegate() : this;
        aepMessage = (AEPMessage) ServiceProvider.getInstance().getUIService().createFullscreenMessage(html, fullscreenMessageDelegate, false, settings);
        aepMessage.setLocalAssetsMap(assetMap);
    }

    /**
     * Dispatch tracking information via a Messaging request content event.
     *
     * @param interaction {@code String} containing the interaction which occurred
     * @param eventType   {@link MessagingEdgeEventType} enum containing the {@link EventType} to be used for the ensuing Edge Event
     */
    public void track(final String interaction, final MessagingEdgeEventType eventType) {
        if (eventType == null) {
            Log.debug(LOG_TAG,
                    "%s - Unable to record a message interaction, MessagingEdgeEventType was null.", SELF_TAG);
            return;
        }
        messagingInternal.handleInAppTrackingInfo(eventType, interaction, this);
    }

    /**
     * Adds a {@link WebViewJavascriptInterface} for the provided message name to the javascript {@link WebView}.
     *
     * @param name     {@link String} the name of the message being passed from javascript
     * @param callback {@code AdobeCallback<String>} to be invoked when the javascript message payload is passed
     */
    public void handleJavascriptMessage(final String name, final AdobeCallback<String> callback) {
        if (StringUtils.isNullOrEmpty(name)) {
            Log.trace(LOG_TAG, "Will not store the callback, no name was provided.");
            return;
        }

        if (scriptHandlers.get(name) != null) {
            Log.trace(LOG_TAG, "Will not create a new WebViewJavascriptInterface, the name is already in use.");
            return;
        }

        // create webview for javascript evaluation if needed, otherwise just add a new js interface to the existing webview
        new Handler(MobileCore.getApplication().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (view == null) {
                    Log.trace(LOG_TAG, "Created new WebView for javascript evaluation.");
                    view = new WebView(MobileCore.getApplication().getApplicationContext());
                    final WebSettings settings = view.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setJavaScriptCanOpenWindowsAutomatically(true);
                }
                final WebViewJavascriptInterface javascriptInterface = new WebViewJavascriptInterface(callback);
                view.addJavascriptInterface(javascriptInterface, name);
                scriptHandlers.put(name, javascriptInterface);
            }
        });
    }

    void evaluateJavascript(final String content) {
        if (StringUtils.isNullOrEmpty(content)) {
            Log.debug(LOG_TAG, "Will not evaluate javascript, it is null or empty.");
            return;
        }

        if (scriptHandlers == null || scriptHandlers.isEmpty()) {
            Log.debug(LOG_TAG, "Will not evaluate javascript, no script handlers have been set.");
            return;
        }

        for (final Map.Entry<String, WebViewJavascriptInterface> entry : scriptHandlers.entrySet()) {
            view.evaluateJavascript(content, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(final String value) {
                    Log.debug(LOG_TAG, "Running javascript callback for javascript function (%s)", entry.getKey());
                    entry.getValue().run(value);
                }
            });
        }
    }

    // ui management
    public void show() {
        if (aepMessage != null) {
            if (autoTrack) {
                track(null, MessagingEdgeEventType.IN_APP_DISPLAY);
            }
            aepMessage.show();
        }
    }

    public void dismiss(final boolean suppressAutoTrack) {
        if (aepMessage != null) {
            if (autoTrack && !suppressAutoTrack) {
                track(null, MessagingEdgeEventType.IN_APP_DISMISS);
            }

            aepMessage.dismiss();
        }
    }

    /**
     * Sample Message payload:
     * "messageSetting": {
     * "schemaVersion": "1.0",
     * "width": "80",
     * "height": "50",
     * "verticalAlign": "center",
     * "verticalInset": "0",
     * "horizontalAlign": "center",
     * "horizontalInset": "0",
     * "uiTakeover": true,
     * "displayAnimation": "top",
     * "dismissAnimation": "top",
     * "backdropColor": "AA00EE",
     * "backdropOpacity": 0.2,
     * "cornerRadius": 15,
     * "gestures": {
     * "swipeUp": "adbinapp://dismiss",
     * "swipeDown": "adbinapp://dismiss",
     * "swipeLeft": "adbinapp://dismiss?interaction=negative",
     * "swipeRight": "adbinapp://dismiss?interaction=positive",
     * "tapBackground": "adbinapp://dismiss"
     * }
     * }
     */
    private MessageSettings addMessageSettings(final AEPMessageSettings.Builder builder, final Map<String, Object> rawSettings) {
        int width, height, verticalInset, horizontalInset;
        String backdropColor;
        float backdropOpacity;
        float cornerRadius;
        boolean uiTakeover;
        MessageAlignment verticalAlign, horizontalAlign;
        MessageAnimation displayAnimation, dismissAnimation;
        Map<MessageGesture, String> gestureMap = new HashMap<>();

        if (rawSettings.get(MobileParametersKeys.WIDTH) != null) {
            width = (int) rawSettings.get(MobileParametersKeys.WIDTH);
        } else {
            width = 100;
        }

        if (rawSettings.get(MobileParametersKeys.HEIGHT) != null) {
            height = (int) rawSettings.get(MobileParametersKeys.HEIGHT);
        } else {
            height = 100;
        }

        if (rawSettings.get(MobileParametersKeys.VERTICAL_ALIGN) != null) {
            verticalAlign = MessageAlignment.valueOf(((String) rawSettings.get(MobileParametersKeys.VERTICAL_ALIGN)).toUpperCase());
        } else {
            verticalAlign = MessageAlignment.CENTER;
        }

        if (rawSettings.get(MobileParametersKeys.VERTICAL_INSET) != null) {
            verticalInset = (int) rawSettings.get(MobileParametersKeys.VERTICAL_INSET);
        } else {
            verticalInset = 0;
        }

        if (rawSettings.get(MobileParametersKeys.HORIZONTAL_ALIGN) != null) {
            horizontalAlign = MessageAlignment.valueOf(((String) rawSettings.get(MobileParametersKeys.HORIZONTAL_ALIGN)).toUpperCase());
        } else {
            horizontalAlign = MessageAlignment.CENTER;
        }

        if (rawSettings.get(MobileParametersKeys.HORIZONTAL_INSET) != null) {
            horizontalInset = (int) rawSettings.get(MobileParametersKeys.HORIZONTAL_INSET);
        } else {
            horizontalInset = 0;
        }

        if (rawSettings.get(MobileParametersKeys.DISPLAY_ANIMATION) != null) {
            displayAnimation = MessageAnimation.valueOf(((String) rawSettings.get(MobileParametersKeys.DISPLAY_ANIMATION)).toUpperCase());
        } else {
            displayAnimation = MessageAnimation.NONE;
        }

        if (rawSettings.get(MobileParametersKeys.DISMISS_ANIMATION) != null) {
            dismissAnimation = MessageAnimation.valueOf(((String) rawSettings.get(MobileParametersKeys.DISMISS_ANIMATION)).toUpperCase());
        } else {
            dismissAnimation = MessageAnimation.NONE;
        }

        if (rawSettings.get(MobileParametersKeys.BACKDROP_COLOR) != null) {
            backdropColor = (String) rawSettings.get(MobileParametersKeys.BACKDROP_COLOR);
        } else {
            backdropColor = "#FFFFFF";
        }

        if (rawSettings.get(MobileParametersKeys.BACKDROP_OPACITY) != null) {
            final double opacity = ((Double) rawSettings.get(MobileParametersKeys.BACKDROP_OPACITY));
            backdropOpacity = (float) opacity;
        } else {
            backdropOpacity = 0.0f;
        }

        if (rawSettings.get(MobileParametersKeys.CORNER_RADIUS) != null) {
            final double radius = ((Double) rawSettings.get(MobileParametersKeys.CORNER_RADIUS));
            cornerRadius = (float) radius;
        } else {
            cornerRadius = 0.0f;
        }

        if (rawSettings.get(MobileParametersKeys.UI_TAKEOVER) != null) {
            uiTakeover = (boolean) rawSettings.get(MobileParametersKeys.UI_TAKEOVER);
        } else {
            uiTakeover = true;
        }

        // we need to convert key strings present in the gestures map to MessageGesture enum keys
        final Map<String, String> stringMap = (Map<String, String>) rawSettings.get(MobileParametersKeys.GESTURES);
        if (!MessagingUtils.isMapNullOrEmpty(stringMap)) {
            for (final Map.Entry<String, String> entry : stringMap.entrySet()) {
                final MessageGesture gesture = MessageGesture.get(entry.getKey());
                gestureMap.put(gesture, entry.getValue());
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
                .setGestures(gestureMap)
                .build();
    }

    /**
     * Gets the message id {@code String} contained in the message execution payload.
     *
     * @param messageDetails {@code Map<String, Object>} containing the message details
     * @return {@code String} containing the message id or null if none was found
     */
    private String getMessageId(Map<String, Object> messageDetails) {
        final Map<String, Object> xdm = (Map<String, Object>) messageDetails.get(MessagingConstants.TrackingKeys._XDM);
        final Map<String, Object> mixins = MessagingUtils.isMapNullOrEmpty(xdm) ? null : (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.MIXINS);
        final Map<String, Object> experience = MessagingUtils.isMapNullOrEmpty(mixins) ? null : (Map<String, Object>) mixins.get(MessagingConstants.TrackingKeys.EXPERIENCE);
        final Map<String, Object> cjm = MessagingUtils.isMapNullOrEmpty(experience) ? null : (Map<String, Object>) experience.get(MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT);
        final Map<String, Object> messageExecution = MessagingUtils.isMapNullOrEmpty(cjm) ? null : (Map<String, Object>) cjm.get(MessagingConstants.TrackingKeys.MESSAGE_EXECUTION);
        return MessagingUtils.isMapNullOrEmpty(messageExecution) ? null : (String) messageExecution.get(MessagingConstants.TrackingKeys.MESSAGE_EXECUTION_ID);
    }
}
