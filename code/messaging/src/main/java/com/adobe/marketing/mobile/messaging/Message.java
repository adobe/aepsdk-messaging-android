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

package com.adobe.marketing.mobile.messaging;

import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.messaging.internal.MessageRequiredFieldMissingException;
import com.adobe.marketing.mobile.messaging.internal.MessagingDelegate;
import com.adobe.marketing.mobile.messaging.internal.MessagingEdgeEventType;
import com.adobe.marketing.mobile.messaging.internal.MessagingExtension;
import com.adobe.marketing.mobile.messaging.internal.PropositionInfo;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAlignment;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAnimation;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageGesture;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the definition of an in-app message and controls its tracking via Experience Edge events.
 */
public class Message extends MessagingDelegate {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "Message";
    private static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
    private static final String MESSAGE_CONSEQUENCE_ID = "id";
    private static final String MESSAGE_CONSEQUENCE_TYPE = "type";
    private static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String VERTICAL_ALIGN = "verticalAlign";
    private static final String VERTICAL_INSET = "verticalInset";
    private static final String HORIZONTAL_ALIGN = "horizontalAlign";
    private static final String HORIZONTAL_INSET = "horizontalInset";
    private static final String UI_TAKEOVER = "uiTakeover";
    private static final String DISPLAY_ANIMATION = "displayAnimation";
    private static final String DISMISS_ANIMATION = "dismissAnimation";
    private static final String BACKDROP_COLOR = "backdropColor";
    private static final String BACKDROP_OPACITY = "backdropOpacity";
    private static final String CORNER_RADIUS = "cornerRadius";
    private static final String GESTURES = "gestures";
    
    private final FullscreenMessage aepMessage;
    private final Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();
    private final Handler webViewHandler;
    // public properties
    public String id;
    public boolean autoTrack = true;
    // private properties
    private WebView webView;
    // package private
    public PropositionInfo propositionInfo; // contains XDM data necessary for tracking in-app interactions with Adobe Journey Optimizer

    /**
     * Constructor.
     * <p>
     * Every {@link Message} requires a {@link #id}, and must be of type "cjmiam".
     * <p>
     * The consequence {@code Map} for a {@code Message} is required to have valid values for the following fields:
     * <ul>
     *     <li>{@value #MESSAGE_CONSEQUENCE_ID} - {@code String} containing the message ID</li>
     *     <li>{@value #MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
     *     <li>{@value #MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Object>} containing details of the Message to be displayed</li>
     * </ul>
     *
     * @param parent             {@link MessagingExtension} instance that created this Message
     * @param consequence        {@link com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence} containing a {@code Message} defining payload
     * @param rawMessageSettings {@code Map<String, Object>} containing the raw message settings found in the "mobileParameters" present in the rule consequence
     * @param assetMap           {@code Map<String, Object>} containing a mapping of a remote image asset URL and it's cached location
     * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
     */
    public Message(final MessagingExtension parent, final RuleConsequence consequence, final Map<String, Object> rawMessageSettings, final Map<String, String> assetMap) throws MessageRequiredFieldMissingException {
        this(parent, consequence, rawMessageSettings, assetMap, null, null);
    }

    @VisibleForTesting
    Message(final MessagingExtension parent, final RuleConsequence consequence, final Map<String, Object> rawMessageSettings, final Map<String, String> assetMap, final WebView webView, final Handler webViewHandler) throws MessageRequiredFieldMissingException {
        messagingExtension = parent;
        this.webView = webView;
        this.webViewHandler = webViewHandler != null ? webViewHandler : new Handler(ServiceProvider.getInstance().getAppContextService().getApplication().getMainLooper());
        final String consequenceType = consequence.getType();

        if (!MESSAGE_CONSEQUENCE_CJM_VALUE.equals(consequenceType)) {
            Log.debug(LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"type\" is (%s) should be of type (cjmiam).", consequence.toString(), consequenceType);
            throw new MessageRequiredFieldMissingException("Required field: \"type\" is not equal to \"cjmiam\".");
        }

        details = consequence.getDetail();
        if (MapUtils.isNullOrEmpty(details)) {
            Log.debug(LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"detail\" is null or empty.", consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
        }

        id = consequence.getId();
        if (StringUtils.isNullOrEmpty(id)) {
            Log.debug(LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"id\" is null or empty.", consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        final String html = (String) details.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create an in-app message, the html payload is null or empty.");
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        final MessageSettings settings = messageSettingsFromMap(rawMessageSettings);
        settings.setParent(this);

        // set the internal Messaging delegate if a custom Messaging delegate is not being used
        if (ServiceProvider.getInstance().getMessageDelegate() == null) {
            ServiceProvider.getInstance().setMessageDelegate(this);
        }

        aepMessage = ServiceProvider.getInstance().getUIService().createFullscreenMessage(html, this, !assetMap.isEmpty(), settings);
        aepMessage.setLocalAssetsMap(assetMap);
    }

    @VisibleForTesting
    Map<String, WebViewJavascriptInterface> getScriptHandlers() {
        return scriptHandlers;
    }

    /**
     * Dispatch tracking information via a Messaging request content event.
     *
     * @param interaction {@code String} containing the interaction which occurred
     * @param eventType   {@link MessagingEdgeEventType} enum containing the Event Type to be used for the ensuing Edge Event
     */
    public void track(final String interaction, final MessagingEdgeEventType eventType) {
        if (eventType == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to record a message interaction, MessagingEdgeEventType was null.");
            return;
        }
        messagingExtension.sendPropositionInteraction(interaction, eventType, this);
    }

    /**
     * Adds a {@link WebViewJavascriptInterface} for the provided message name to the javascript {@link WebView}.
     *
     * @param name     {@link String} the name of the message being passed from javascript
     * @param callback {@code AdobeCallback<String>} to be invoked when the javascript message payload is passed
     */
    public void handleJavascriptMessage(final String name, final AdobeCallback<String> callback) {
        if (StringUtils.isNullOrEmpty(name)) {
            Log.trace(LOG_TAG, SELF_TAG, "Will not store the callback, no name was provided.");
            return;
        }

        if (scriptHandlers.get(name) != null) {
            Log.trace(LOG_TAG, SELF_TAG, "Will not create a new WebViewJavascriptInterface, the name is already in use.");
            return;
        }

        // add a new js interface to the iam webview
        webViewHandler.post(new Runnable() {
            @Override
            public void run() {
                // retrieve the webview created for the iam
                getWebView();

                if (webView == null) {
                    Log.debug(LOG_TAG, SELF_TAG, "Will not add a javascript interface, the MessageWebView is null.");
                    return;
                }

                final WebViewJavascriptInterface javascriptInterface = new WebViewJavascriptInterface(callback);
                webView.addJavascriptInterface(javascriptInterface, name);
                scriptHandlers.put(name, javascriptInterface);
            }
        });
    }

    /**
     * Returns the {@link WebView} to allow manual integration of the in-app message.
     *
     * @return a {@code WebView} containing the Messaging extension in-app message
     */
    public WebView getWebView() {
        if (webView == null) {
            webView = aepMessage.getWebView();
        }
        return webView;
    }

    /**
     * Evaluates the passed in {@code String} content containing javascript code by calling
     * {@link WebView#evaluateJavascript(String, ValueCallback)} in the created {@code WebView}.
     * Any output from the executed javascript code will be returned in an {@link AdobeCallback}
     * previously set in a call to {@link #handleJavascriptMessage(String, AdobeCallback)}.
     *
     * @param content {@code String} containing the javascript code to be executed
     */
    public void evaluateJavascript(final String content) {
        if (StringUtils.isNullOrEmpty(content)) {
            Log.debug(LOG_TAG, SELF_TAG, "Will not evaluate javascript, it is null or empty.");
            return;
        }

        if (scriptHandlers == null || scriptHandlers.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG, "Will not evaluate javascript, no script handlers have been set.");
            return;
        }

        if (webView == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Will not evaluate javascript, the MessageWebView is null.");
            return;
        }

        for (final Map.Entry<String, WebViewJavascriptInterface> entry : scriptHandlers.entrySet()) {
            webView.evaluateJavascript(content, value -> {
                Log.debug(LOG_TAG, SELF_TAG, "Running javascript callback for javascript function (%s)", entry.getKey());
                entry.getValue().run(value);
            });
        }
    }

    // ui management
    public void show() {
        show(false);
    }

    public void show(final boolean withMessagingDelegateControl) {
        if (aepMessage != null) {
            if (autoTrack) {
                track(null, MessagingEdgeEventType.IN_APP_DISPLAY);
            }
            aepMessage.show(withMessagingDelegateControl);
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

    public void trigger() {
        if (aepMessage != null) {
            if (autoTrack) {
                track(null, MessagingEdgeEventType.IN_APP_TRIGGER);
            }
        }
    }

    /**
     * Sample mobile parameters payload represented by a MessageSettings object:
     {
        "mobileParameters": {
            "schemaVersion": "1.0",
            "width": 80,
            "height": 50,
            "verticalAlign": "center",
            "verticalInset": 0,
            "horizontalAlign": "center",
            "horizontalInset": 0,
            "uiTakeover": true,
            "displayAnimation": "top",
            "dismissAnimation": "top",
            "backdropColor": "000000", // RRGGBB
            "backdropOpacity: 0.3,
            "cornerRadius": 15,
            "gestures": {
                "swipeUp": "adbinapp://dismiss",
                "swipeDown": "adbinapp://dismiss",
                "swipeLeft": "adbinapp://dismiss?interaction=negative",
                "swipeRight": "adbinapp://dismiss?interaction=positive",
                "tapBackground": "adbinapp://dismiss"
             }
        }
     }
     */
    private MessageSettings messageSettingsFromMap(final Map<String, Object> rawSettings) {
        int width, height, verticalInset, horizontalInset;
        String backdropColor;
        float backdropOpacity;
        float cornerRadius;
        boolean uiTakeover;
        MessageAlignment verticalAlign, horizontalAlign;
        MessageAnimation displayAnimation, dismissAnimation;
        Map<MessageGesture, String> gestureMap = new HashMap<>();

        if (rawSettings.get(WIDTH) != null) {
            width = (int) rawSettings.get(WIDTH);
        } else {
            width = 100;
        }

        if (rawSettings.get(HEIGHT) != null) {
            height = (int) rawSettings.get(HEIGHT);
        } else {
            height = 100;
        }

        if (rawSettings.get(VERTICAL_ALIGN) != null) {
            verticalAlign = MessageAlignment.valueOf(((String) rawSettings.get(VERTICAL_ALIGN)).toUpperCase());
        } else {
            verticalAlign = MessageAlignment.CENTER;
        }

        if (rawSettings.get(VERTICAL_INSET) != null) {
            verticalInset = (int) rawSettings.get(VERTICAL_INSET);
        } else {
            verticalInset = 0;
        }

        if (rawSettings.get(HORIZONTAL_ALIGN) != null) {
            horizontalAlign = MessageAlignment.valueOf(((String) rawSettings.get(HORIZONTAL_ALIGN)).toUpperCase());
        } else {
            horizontalAlign = MessageAlignment.CENTER;
        }

        if (rawSettings.get(HORIZONTAL_INSET) != null) {
            horizontalInset = (int) rawSettings.get(HORIZONTAL_INSET);
        } else {
            horizontalInset = 0;
        }

        if (rawSettings.get(DISPLAY_ANIMATION) != null) {
            displayAnimation = MessageAnimation.valueOf(((String) rawSettings.get(DISPLAY_ANIMATION)).toUpperCase());
        } else {
            displayAnimation = MessageAnimation.NONE;
        }

        if (rawSettings.get(DISMISS_ANIMATION) != null) {
            dismissAnimation = MessageAnimation.valueOf(((String) rawSettings.get(DISMISS_ANIMATION)).toUpperCase());
        } else {
            dismissAnimation = MessageAnimation.NONE;
        }

        if (rawSettings.get(BACKDROP_COLOR) != null) {
            backdropColor = (String) rawSettings.get(BACKDROP_COLOR);
        } else {
            backdropColor = "#FFFFFF";
        }

        if (rawSettings.get(BACKDROP_OPACITY) != null) {
            final double opacity = ((double) rawSettings.get(BACKDROP_OPACITY));
            backdropOpacity = (float) opacity;
        } else {
            backdropOpacity = 0.0f;
        }

        if (rawSettings.get(CORNER_RADIUS) != null) {
            final Integer radius = (Integer) rawSettings.get(CORNER_RADIUS);
            cornerRadius = radius.floatValue();
        } else {
            cornerRadius = 0.0f;
        }

        if (rawSettings.get(UI_TAKEOVER) != null) {
            uiTakeover = (boolean) rawSettings.get(UI_TAKEOVER);
        } else {
            uiTakeover = true;
        }

        // we need to convert key strings present in the gestures map to MessageGesture enum keys
        final Map<String, String> stringMap = (Map<String, String>) rawSettings.get(GESTURES);
        if (!MapUtils.isNullOrEmpty(stringMap)) {
            for (final Map.Entry<String, String> entry : stringMap.entrySet()) {
                final MessageGesture gesture = MessageGesture.get(entry.getKey());
                gestureMap.put(gesture, entry.getValue());
            }
        }

        MessageSettings settings = new MessageSettings();
        settings.setWidth(width);
        settings.setHeight(height);
        settings.setVerticalInset(verticalInset);
        settings.setHorizontalInset(horizontalInset);
        settings.setVerticalAlign(verticalAlign);
        settings.setHorizontalAlign(horizontalAlign);
        settings.setDisplayAnimation(displayAnimation);
        settings.setDismissAnimation(dismissAnimation);
        settings.setBackdropColor(backdropColor);
        settings.setBackdropOpacity(backdropOpacity);
        settings.setCornerRadius(cornerRadius);
        settings.setUiTakeover(uiTakeover);
        settings.setGestures(gestureMap);

        return settings;
    }
}
