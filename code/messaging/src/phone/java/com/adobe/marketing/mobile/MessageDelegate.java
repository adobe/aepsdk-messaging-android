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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MessageDelegate implements UIService.FullscreenMessageDelegate {
    private final static String SELF_TAG = "MessageDelegate";
    private final static String AMPERSAND = "&";
    private final static String EXPECTED_JAVASCRIPT_PARAM = "js=";
    // public properties
    public String messageId;
    public boolean autoTrack = true;
    // internal properties
    MessagingInternal messagingInternal;
    Map<String, Object> details;

    /**
     * Dispatch tracking information via a Messaging request content event.
     */
    public void track(final String interactionType) {
        if(StringUtils.isNullOrEmpty(interactionType)) {
            Log.debug(LOG_TAG,
                    "%s - Unable to record a message interaction - interaction string was null or empty.", SELF_TAG);
            return;
        }

        final HashMap<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, messageId);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, interactionType);

        final Event event = new Event.Builder(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_EDGE_EVENT, MessagingConstants.EventType.MESSAGING, MessagingConstants.EventSource.REQUEST_CONTENT).setEventData(eventData).build();
        Log.debug(LOG_TAG, "%s - Tracking interaction (%s) for message id %s", SELF_TAG, interactionType, messageId);
        messagingInternal.handleInAppTrackingInfo(event);
    }

    /**
     * Requests that the {@code UIService} show this {@code url}.
     *
     * @param url {@link String} containing url to be shown
     */
    protected void openUrl(final String url) {
        final UIService uiService = MessagingUtils.getUIService();

        if (uiService == null || !uiService.showUrl(url)) {
            Log.debug(LOG_TAG, "%s - Could not open URL (%s)", SELF_TAG, url);
        }
    }

    // javascript handling POC
    /**
     * Attempts to run the provided javascript code
     *
     * @param javascript {@link String} containing javascript code to be executed
     */
    protected void loadJavascript(final String javascript) {
        final WebView jsWebview = new WebView(App.getAppContext());
        final WebSettings settings = jsWebview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        if (jsWebview != null) {
            jsWebview.evaluateJavascript(javascript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.debug(LOG_TAG,"Javascript callback: " + value);
                }
            });
        }
    }
    // ============================================================================================
    // FullscreenMessageDelegate implementation
    // ============================================================================================
    @Override
    public void onShow(final UIService.FullscreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message shown.", SELF_TAG);
    }

    @Override
    public void onDismiss(final UIService.FullscreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message dismissed.", SELF_TAG);
        final AEPMessageSettings aepMessageSettings = (AEPMessageSettings) fullscreenMessage.getSettings();
        final Message message = (Message) aepMessageSettings.getParent();
        message.dismiss();
    }

    @Override
    public boolean shouldShowMessage(final UIService.FullscreenMessage fullscreenMessage) {
        return true;
    }

    /**
     * Invoked when a {@link AEPMessage} is attempting to load a URL.
     *
     * @param fullscreenMessage the {@link UIService.FullscreenMessage} instance
     * @param urlString {@link String} containing the URL being loaded by the {@code AEPMessage}
     *
     * @return true if the SDK wants to handle the URL
     */
    @Override
    public boolean overrideUrlLoad(final UIService.FullscreenMessage fullscreenMessage, final String urlString) {
        Log.trace(LOG_TAG, "%s - Fullscreen overrideUrlLoad callback received with url (%s)", SELF_TAG, urlString);

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(LOG_TAG, "%s - Cannot process provided URL string, it is null or empty.", SELF_TAG);
            return true;
        }

        URI uri;

        // JS poc
        // we need to url encode any javascript if present in the url
        String localUrlString = urlString;
        final String[] tokens = urlString.split(AMPERSAND);
        if (tokens[tokens.length - 1].contains(EXPECTED_JAVASCRIPT_PARAM)) {
            try {
                // encode the content after "js="
                final String urlEncodedJavascript = URLEncoder.encode(tokens[tokens.length - 1].substring(3), StandardCharsets.UTF_8.toString());
                localUrlString = tokens[0] + AMPERSAND + EXPECTED_JAVASCRIPT_PARAM + urlEncodedJavascript;
                // the UrlEncoder replaces spaces with "+". we need to manually encode "+" to "%20"".
                localUrlString = localUrlString.replace("+", "%20");
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                Log.debug(LOG_TAG, "%s - Invalid encoding type (%s), javascript will be ignored.", SELF_TAG, StandardCharsets.UTF_8);
            }
        }

        try {
            uri = new URI(localUrlString);
        } catch (URISyntaxException ex) {
            Log.debug(LOG_TAG, "%s - Invalid message URI found (%s).", SELF_TAG, urlString);
            return true;
        }

        // check adbinapp scheme
        final String messageScheme = uri.getScheme();

        if (!messageScheme.equals(MessagingConstants.MESSAGING_SCHEME.ADOBE_INAPP)) {
            Log.debug(LOG_TAG, "%s - Invalid message scheme found in URI. (%s)", SELF_TAG, urlString);
            return false;
        }

        // Populate message data
        final String query = uri.getQuery();
        final Map<String, String> messageData = UrlUtilities.extractQueryParameters(query);

        final AEPMessageSettings aepMessageSettings = (AEPMessageSettings) fullscreenMessage.getSettings();
        final Message message = (Message) aepMessageSettings.getParent();

        if (messageData != null && !messageData.isEmpty()) {
            // handle optional tracking
            final String interaction = messageData.get(MessagingConstants.MESSAGING_SCHEME.INTERACTION);
            if (!StringUtils.isNullOrEmpty(interaction)) {
                // ensure we have the MessagingInternal class available for tracking
                messagingInternal = message.messagingInternal;
                messageId = message.messageId;
                if(messagingInternal != null) {
                    track(interaction);
                }
            }

            final String deeplink = messageData.get("link");
            if (!StringUtils.isNullOrEmpty(deeplink)) {
                openUrl(deeplink);
            }

            // JS poc
            final String javasscript = messageData.get("js");
            if (!StringUtils.isNullOrEmpty(javasscript)) {
                loadJavascript(javasscript);
            }
        }

        final String host = uri.getHost();
        if ((host.equals(MessagingConstants.MESSAGING_SCHEME.PATH_DISMISS)) || (host.equals(MessagingConstants.MESSAGING_SCHEME.PATH_CANCEL))) {
            message.dismiss();
        }

        return true;
    }

    @Override
    public void onShowFailure() {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message failed to show.", SELF_TAG);
    }
}
