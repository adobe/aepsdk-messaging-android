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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.IAM_HISTORY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.MESSAGE_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.TRACKING_ACTION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_IAM;

import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.WebViewJavascriptInterface;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAlignment;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageAnimation;
import com.adobe.marketing.mobile.services.ui.MessageSettings.MessageGesture;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.MobileParametersKeys;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the definition of an in-app message and controls its tracking via Experience Edge events.
 */
class InternalMessage extends MessagingFullscreenMessageDelegate implements Message {
    private final static String SELF_TAG = "Message";
    private final static int FILL_SCREEN = 100;
    private final Map<String, WebViewJavascriptInterface> scriptHandlers;
    private final Handler webViewHandler;
    private final String id;
    private final MessagingExtension messagingExtension;
    private FullscreenMessage aepMessage;
    private WebView webView;
    private boolean autoTrack = true;
    // package private
    PropositionInfo propositionInfo; // contains XDM data necessary for tracking in-app interactions with Adobe Journey Optimizer
    Map<String, Object> details;

    /**
     * Constructor.
     * <p>
     * Every {@link InternalMessage} requires a {@link #id}, and must be of type "cjmiam".
     * <p>
     * The consequence {@code Map} for a {@code InternalMessage} is required to have valid values for the following fields:
     * <ul>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@code String} containing the message ID</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
     *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Object>} containing details of the Message to be displayed</li>
     * </ul>
     *
     * @param parent             {@link MessagingExtension} instance that created this Message
     * @param consequence        {@link com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence} containing a {@code InternalMessage} defining payload
     * @param rawMessageSettings {@code Map<String, Object>} containing the raw message settings found in the "mobileParameters" present in the rule consequence
     * @param assetMap           {@code Map<String, Object>} containing a mapping of a remote image asset URL and its cached location
     * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
     */
    InternalMessage(final MessagingExtension parent, final RuleConsequence consequence, final Map<String, Object> rawMessageSettings, final Map<String, String> assetMap) throws MessageRequiredFieldMissingException {
        this(parent, consequence, rawMessageSettings, assetMap, null, null, null);
    }

    @VisibleForTesting
    InternalMessage(final MessagingExtension parent, final RuleConsequence consequence, final Map<String, Object> rawMessageSettings, final Map<String, String> assetMap, final WebView webView, final Handler webViewHandler, final Map<String, WebViewJavascriptInterface> scriptHandlers) throws MessageRequiredFieldMissingException {
        messagingExtension = parent;
        this.webView = webView;
        this.webViewHandler = webViewHandler != null ? webViewHandler : new Handler(ServiceProvider.getInstance().getAppContextService().getApplication().getMainLooper());
        this.scriptHandlers = scriptHandlers != null ? scriptHandlers : new HashMap<>();

        id = consequence.getId();
        if (StringUtils.isNullOrEmpty(id)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"id\" is null or empty.", consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
        }

        details = consequence.getDetail();
        if (MapUtils.isNullOrEmpty(details)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"detail\" is null or empty.", consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
        }

        final String schemaType = DataReader.optString(details, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
        if (!SCHEMA_IAM.equals(schemaType)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"schema\" is (%s) should be of type (%S).", consequence.toString(), schemaType, SCHEMA_IAM);
            throw new MessageRequiredFieldMissingException("Required field: \"schema\" is not equal to \"https://ns.adobe.com/personalization/message/in-app\".");
        }

        final Map<String, Object> data = DataReader.optTypedMap(Object.class, details, MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, null);
        if (MapUtils.isNullOrEmpty(data)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid consequence (%s). Required field \"data\" is null or empty.", consequence.toString());
            throw new MessageRequiredFieldMissingException("Required field: \"data\" is null or empty.");
        }

        final String html = DataReader.optString(data, MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, "");
        if (StringUtils.isNullOrEmpty(html)) {
            Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create an in-app message, the html payload is null or empty.");
            throw new MessageRequiredFieldMissingException("Required field: \"html\" is null or empty.");
        }

        final MessageSettings settings = messageSettingsFromMap(rawMessageSettings);
        settings.setParent(this);

        final UIService uiService = ServiceProvider.getInstance().getUIService();
        if (uiService == null) {
            Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "The UIService is unavailable. Aborting in-app message creation.");
            return;
        }
        aepMessage = uiService.createFullscreenMessage(html, this, !assetMap.isEmpty(), settings);
        if (aepMessage == null) {
            Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Error occurred during in-app message creation.");
            return;
        }
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
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction, MessagingEdgeEventType was null.");
            return;
        }
        if (propositionInfo == null) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction (%s), PropositionInfo is not found for message (%s)", eventType.getPropositionEventType(), id);
            return;
        }
        final PropositionInteraction propositionInteraction = new PropositionInteraction(eventType,
                interaction == null ? "" : interaction, propositionInfo, null, null);
        final Map<String, Object> propositionInteractionXdm = propositionInteraction.getPropositionInteractionXDM();
        messagingExtension.sendPropositionInteraction(propositionInteractionXdm);
    }

    /**
     * Adds a {@link WebViewJavascriptInterface} for the provided handler name to the {@link WebView}.
     *
     * @param name     {@link String} the name of the javascript handler
     * @param callback {@code AdobeCallback<String>} to be invoked with the output of the javascript code
     */
    public void handleJavascriptMessage(final String name, final AdobeCallback<String> callback) {
        if (StringUtils.isNullOrEmpty(name)) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Will not store the callback, no name was provided.");
            return;
        }

        if (scriptHandlers.get(name) != null) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Will not create a new WebViewJavascriptInterface, the name is already in use.");
            return;
        }

        // add a new js interface to the webview
        webViewHandler.post(() -> {
            // retrieve the webview created for the iam
            getWebView();

            if (webView == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not add a javascript interface, the MessageWebView is null.");
                return;
            }

            final WebViewJavascriptInterface javascriptInterface = new WebViewJavascriptInterface(callback);
            webView.addJavascriptInterface(javascriptInterface, name);
            scriptHandlers.put(name, javascriptInterface);
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
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not evaluate javascript, it is null or empty.");
            return;
        }

        String urlDecodedString;
        try {
            urlDecodedString = URLDecoder.decode(content, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Exception occurred decoding url string: (%s).", e.getMessage());
            return;
        }

        if (scriptHandlers == null || scriptHandlers.isEmpty()) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not evaluate javascript, no script handlers have been set.");
            return;
        }

        if (webView == null) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not evaluate javascript, the MessageWebView is null.");
            return;
        }

        for (final Map.Entry<String, WebViewJavascriptInterface> entry : scriptHandlers.entrySet()) {
            webView.evaluateJavascript(urlDecodedString, value -> {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Running javascript callback for javascript function (%s)", entry.getKey());
                entry.getValue().run(value);
            });
        }
    }

    // ui management
    public void show() {
        show(false);
    }

    void show(final boolean withMessagingDelegateControl) {
        if (aepMessage != null) {
            aepMessage.show(withMessagingDelegateControl);
        }
    }

    public void dismiss() {
        if (aepMessage != null) {
            aepMessage.dismiss();
        }
    }

    /**
     * Called when a {@code Message} is triggered - i.e. its conditional criteria have been met.
     */
    void trigger() {
        if (aepMessage != null) {
            if (autoTrack) {
                track(null, MessagingEdgeEventType.TRIGGER);
            }
            recordEventHistory(null, MessagingEdgeEventType.TRIGGER);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MessagingExtension getParent() {
        return messagingExtension;
    }

    @Override
    public boolean getAutoTrack() {
        return autoTrack;
    }

    @Override
    public void setAutoTrack(final boolean useAutoTrack) {
        this.autoTrack = useAutoTrack;
    }

    /**
     * Dispatches an event to be recorded in Event History.
     *
     * @param interaction {@code String}  if provided, adds a custom interaction to the hash
     * @param eventType {@link MessagingEdgeEventType} to be recorded
     */
    void recordEventHistory(final String interaction, final MessagingEdgeEventType eventType) {
        if (eventType == null) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to write event history event, MessagingEdgeEventType was null for message (%s).", id);
            return;
        }
        if (propositionInfo == null) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to write event history event (%s), PropositionInfo is not found for message (%s)", eventType.getPropositionEventType(), id);
            return;
        }
        // create maps for event history
        final Map<String, String> iamHistoryMap = new HashMap<>();
        iamHistoryMap.put(EVENT_TYPE, eventType.getPropositionEventType());
        iamHistoryMap.put(MESSAGE_ID, propositionInfo.activityId);
        iamHistoryMap.put(TRACKING_ACTION, (StringUtils.isNullOrEmpty(interaction) ? "" : interaction));

        // Create the mask for storing event history
        final Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put(IAM_HISTORY, iamHistoryMap);
        final String[] mask = {MessagingConstants.EventMask.Mask.EVENT_TYPE, MessagingConstants.EventMask.Mask.MESSAGE_ID, MessagingConstants.EventMask.Mask.TRACKING_ACTION};

        InternalMessagingUtils.sendEvent(MessagingConstants.EventName.EVENT_HISTORY_WRITE,
                MessagingConstants.EventType.MESSAGING,
                MessagingConstants.EventSource.EVENT_HISTORY_WRITE,
                eventHistoryData,
                mask,
                messagingExtension.getApi());
    }

    /**
     * Sample mobile parameters payload represented by a MessageSettings object:
     * {
     * "mobileParameters": {
     * "schemaVersion": "1.0",
     * "width": 80,
     * "height": 50,
     * "verticalAlign": "center",
     * "verticalInset": 0,
     * "horizontalAlign": "center",
     * "horizontalInset": 0,
     * "uiTakeover": true,
     * "displayAnimation": "top",
     * "dismissAnimation": "top",
     * "backdropColor": "000000", // RRGGBB
     * "backdropOpacity: 0.3,
     * "cornerRadius": 15,
     * "gestures": {
     * "swipeUp": "adbinapp://dismiss",
     * "swipeDown": "adbinapp://dismiss",
     * "swipeLeft": "adbinapp://dismiss?interaction=negative",
     * "swipeRight": "adbinapp://dismiss?interaction=positive",
     * "tapBackground": "adbinapp://dismiss"
     * }
     * }
     * }
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

        width = DataReader.optInt(rawSettings, MobileParametersKeys.WIDTH, FILL_SCREEN);
        height = DataReader.optInt(rawSettings, MobileParametersKeys.HEIGHT, FILL_SCREEN);
        verticalAlign = MessageAlignment.valueOf((DataReader.optString(rawSettings, MobileParametersKeys.VERTICAL_ALIGN, "center").toUpperCase()));
        verticalInset = DataReader.optInt(rawSettings, MobileParametersKeys.VERTICAL_INSET, 0);
        horizontalAlign = MessageAlignment.valueOf((DataReader.optString(rawSettings, MobileParametersKeys.HORIZONTAL_ALIGN, "center").toUpperCase()));
        horizontalInset = DataReader.optInt(rawSettings, MobileParametersKeys.HORIZONTAL_INSET, 0);
        displayAnimation = MessageAnimation.valueOf((DataReader.optString(rawSettings, MobileParametersKeys.DISPLAY_ANIMATION, "none").toUpperCase()));
        dismissAnimation = MessageAnimation.valueOf((DataReader.optString(rawSettings, MobileParametersKeys.DISMISS_ANIMATION, "none").toUpperCase()));
        backdropColor = DataReader.optString(rawSettings, MobileParametersKeys.BACKDROP_COLOR, "#FFFFFF");
        backdropOpacity = DataReader.optFloat(rawSettings, MobileParametersKeys.BACKDROP_OPACITY, 0.0f);
        cornerRadius = DataReader.optFloat(rawSettings, MobileParametersKeys.CORNER_RADIUS, 0.0f);
        uiTakeover = DataReader.optBoolean(rawSettings, MobileParametersKeys.UI_TAKEOVER, true);

        // we need to convert key strings present in the gestures map to MessageGesture enum keys
        final Map<String, String> stringMap = DataReader.optStringMap(rawSettings, MobileParametersKeys.GESTURES, null);
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

    @VisibleForTesting
    void setWebView(final WebView webView) {
        this.webView = webView;
    }
}