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
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AEPMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the Messaging extension's internal implementation of the {@link FullscreenMessageDelegate}.
 * Additionally, this class handles the dispatching of Experience Edge tracking events as well as opening URL's
 * and loading any javascript code for the {@link Message} class.
 */
public class MessageDelegate implements FullscreenMessageDelegate {
    private final static String SELF_TAG = "MessageDelegate";
    private final static String AMPERSAND = "&";
    private final static String EXPECTED_JAVASCRIPT_PARAM = "js=";
    private final static String ADOBE_DEEPLINK = "adb_deeplink";
    // public properties
    public String messageId;
    public boolean autoTrack = true;
    // internal properties
    MessagingInternal messagingInternal;
    Map<String, Object> details = new HashMap<>();
    // private property
    private static Map<String, WebViewJavascriptInterface> scriptHandlers = new HashMap<>();
    private static WebView jsWebView;
    private Handler handler;

    /**
     * Determines if the passed in {@code String} link is a deeplink. If not,
     * the {@link UIService} is used to load the link.
     *
     * @param link {@link String} containing the deeplink to load or url to be shown
     */
    protected void openUrl(final FullscreenMessage message, final String link) {
        if (StringUtils.isNullOrEmpty(link)) {
            Log.trace(LOG_TAG, "Will not open url, it is null or empty.");
            return;
        }
        // if we have a deeplink, open the url via an intent
        if (link.contains(ADOBE_DEEPLINK)) {
            Log.debug(LOG_TAG, "Opening deeplink %s.", SELF_TAG, link);
            message.openUrl(link);
            return;
        }
        // otherwise open the url with the ui service
        final UIService uiService = ServiceProvider.getInstance().getUIService();

        if (uiService == null || !uiService.showUrl(link)) {
            Log.debug(LOG_TAG, "%s - Could not open URL (%s)", SELF_TAG, link);
        }
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

        if(scriptHandlers.get(name) != null) {
            Log.trace(LOG_TAG, "Will not create a new WebViewJavascriptInterface, the name is already in use.");
            return;
        }

        // create webview for javascript evaluation if needed, otherwise just add a new js interface to the existing webview
        new Handler(MobileCore.getApplication().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (jsWebView == null) {
                    Log.trace(LOG_TAG, "Created new WebView for javascript evaluation.");
                    jsWebView = new WebView(MobileCore.getApplication().getApplicationContext());
                    final WebSettings settings = jsWebView.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setJavaScriptCanOpenWindowsAutomatically(true);
                }
                final WebViewJavascriptInterface javascriptInterface = new WebViewJavascriptInterface(callback);
                jsWebView.addJavascriptInterface(javascriptInterface, name);
                scriptHandlers.put(name, javascriptInterface);
            }
        });
    }

    void evaluateJavascript(final String content) {
        for (final Map.Entry<String, WebViewJavascriptInterface> entry : scriptHandlers.entrySet()) {
            jsWebView.evaluateJavascript(content, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(final String value) {
                    Log.trace(LOG_TAG, "Running javascript callback for %s.", entry.getKey());
                    entry.getValue().run(value);
                }
            });
        }
    }

    // ============================================================================================
    // FullscreenMessageDelegate implementation
    // ============================================================================================
    @Override
    public void onShow(final FullscreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message shown.", SELF_TAG);
    }

    @Override
    public void onDismiss(final FullscreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message dismissed.", SELF_TAG);
        final MessageSettings aepMessageSettings = ((AEPMessage) fullscreenMessage).getSettings();
        final Message message = (Message) aepMessageSettings.getParent();
        message.dismiss();
    }

    @Override
    public boolean shouldShowMessage(final FullscreenMessage fullscreenMessage) {
        return true;
    }

    /**
     * Invoked when a {@link AEPMessage} is attempting to load a URL.
     *
     * @param fullscreenMessage the {@link FullscreenMessage} instance
     * @param urlString         {@link String} containing the URL being loaded by the {@code AEPMessage}
     * @return true if the SDK wants to handle the URL
     */
    @Override
    public boolean overrideUrlLoad(final FullscreenMessage fullscreenMessage, final String urlString) {
        Log.trace(LOG_TAG, "%s - Fullscreen overrideUrlLoad callback received with url (%s)", SELF_TAG, urlString);

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(LOG_TAG, "%s - Cannot process provided URL string, it is null or empty.", SELF_TAG);
            return true;
        }

        URI uri;

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

        final MessageSettings aepMessageSettings = ((AEPMessage) fullscreenMessage).getSettings();
        final Message message = (Message) aepMessageSettings.getParent();

        if (messageData != null && !messageData.isEmpty()) {
            // handle optional tracking
            final String interaction = messageData.get(MessagingConstants.MESSAGING_SCHEME.INTERACTION);
            if (!StringUtils.isNullOrEmpty(interaction)) {
                // ensure we have the MessagingInternal class available for tracking
                messagingInternal = message.messagingInternal;
                messageId = message.messageId;
                if (messagingInternal != null) {
                    message.track(interaction, MessagingEdgeEventType.IN_APP_INTERACT);
                }
            }

            // handle optional deep link
            final String url = messageData.get(MessagingConstants.MESSAGING_SCHEME.LINK);
            if (!StringUtils.isNullOrEmpty(url)) {
                openUrl(fullscreenMessage, url);
            }

            // handle optional javascript code to be executed
            final String javasscript = messageData.get(MessagingConstants.MESSAGING_SCHEME.JS);
            if (!StringUtils.isNullOrEmpty(javasscript)) {
                evaluateJavascript(javasscript);
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