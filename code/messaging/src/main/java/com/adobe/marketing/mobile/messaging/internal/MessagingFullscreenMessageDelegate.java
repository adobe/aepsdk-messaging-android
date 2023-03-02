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

package com.adobe.marketing.mobile.messaging.internal;

import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the Messaging extension implementation of {@link FullscreenMessageDelegate}.
 */
class MessagingFullscreenMessageDelegate implements FullscreenMessageDelegate {
    private final static String SELF_TAG = "MessagingFullscreenMessageDelegate";
    /**
     * Invoked when the in-app message is displayed.
     *
     * @param fullscreenMessage the {@link FullscreenMessage} being displayed
     */
    @Override
    public void onShow(final FullscreenMessage fullscreenMessage) {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message shown.");
    }

    /**
     * Invoked when the in-app message is dismissed.
     *
     * @param fullscreenMessage the {@link FullscreenMessage} being dismissed
     */
    @Override
    public void onDismiss(final FullscreenMessage fullscreenMessage) {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message dismissed.");
    }

    /**
     * Invoked when the in-app message failed to be displayed.
     */
    @Override
    public void onShowFailure() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message failed to show.");
    }

    /**
     * Invoked when a {@link FullscreenMessage} is attempting to load a URL.
     *
     * @param fullscreenMessage the {@link FullscreenMessage} instance
     * @param urlString         {@link String} containing the URL being loaded by the {@code AEPMessage}
     * @return true if the SDK wants to handle the URL
     */
    @Override
    public boolean overrideUrlLoad(final FullscreenMessage fullscreenMessage, final String urlString) {
        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen overrideUrlLoad callback received with url (%s)", urlString);

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Cannot process provided URL string, it is null or empty.");
            return true;
        }

        URI uri;

        // we need to url encode any javascript if present in the url
        String localUrlString = urlString;
        if (urlString.contains(MessagingConstants.QueryParameters.EXPECTED_JAVASCRIPT_PARAM)) {
            localUrlString = encodeJavascript(urlString);
        }

        try {
            uri = new URI(localUrlString);
        } catch (final URISyntaxException ex) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message URI found (%s), exception is: %s.", urlString, ex.getMessage());
            return true;
        }

        // check adbinapp scheme
        final String messageScheme = uri.getScheme();

        if (messageScheme == null || !messageScheme.equals(MessagingConstants.QueryParameters.ADOBE_INAPP)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message scheme found in URI. (%s)", urlString);
            return false;
        }

        // Populate message data
        final String query = uri.getQuery();
        final Map<String, String> messageData = extractQueryParameters(query);

        final MessageSettings messageSettings = fullscreenMessage.getMessageSettings();
        final Message message = (Message) messageSettings.getParent();

        if (!MapUtils.isNullOrEmpty(messageData)) {
            // handle optional tracking
            final String interaction = messageData.get(MessagingConstants.QueryParameters.INTERACTION);
            if (!StringUtils.isNullOrEmpty(interaction)) {
                // ensure we have the MessagingExtension class available for tracking
                final Object messagingExtension = message.getParent();
                if (messagingExtension != null) {
                    message.track(interaction, MessagingEdgeEventType.IN_APP_INTERACT);
                }
            }

            // handle optional deep link
            final String url = messageData.get(MessagingConstants.QueryParameters.LINK);
            if (!StringUtils.isNullOrEmpty(url)) {
                openUrl(fullscreenMessage, url);
            }

            // handle optional javascript code to be executed
            final String javascript = messageData.get(MessagingConstants.QueryParameters.JAVASCRIPT_QUERY_KEY);
            if (!StringUtils.isNullOrEmpty(javascript)) {
                message.evaluateJavascript(javascript);
            }
        }

        final String host = uri.getHost();
        if ((host.equals(MessagingConstants.QueryParameters.PATH_DISMISS)) || (host.equals(MessagingConstants.QueryParameters.PATH_CANCEL))) {
            message.dismiss(true);
        }

        return true;
    }

    // ============================================================================================
    // FullscreenMessageDelegate implementation helper functions
    // ============================================================================================

    /**
     * Determines if the passed in {@code String} link is a deeplink. If not,
     * the {@link UIService} is used to load the link.
     *
     * @param url {@link String} containing the deeplink to load or url to be shown
     */
    void openUrl(final FullscreenMessage message, final String url) {
        if (StringUtils.isNullOrEmpty(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG,  "Will not open URL, it is null or empty.");
            return;
        }

        // if we have a deeplink, open the url via an intent
        if (url.contains(MessagingConstants.QueryParameters.DEEPLINK)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Opening deeplink (%s).", url);
            message.openUrl(url);
            return;
        }

        // open the url with the ui service.
        final UIService uiService = ServiceProvider.getInstance().getUIService();
        if (uiService == null || !uiService.showUrl(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Could not open URL (%s)", url);
        }
    }

    private String encodeJavascript(final String urlString) {
        final String[] queryTokens = urlString.split("\\?");
        final Map<String, String> queryParams = extractQueryParameters(queryTokens[1]);
        final StringBuilder processedUrlStringBuilder = new StringBuilder(queryTokens[0]);
        try {
            final String javascript = queryParams.get(MessagingConstants.QueryParameters.JAVASCRIPT_QUERY_KEY);
            if (StringUtils.isNullOrEmpty(javascript)) {
                return null;
            }
            String urlEncodedJavascript = URLEncoder.encode(javascript, StandardCharsets.UTF_8.toString());
            // the UrlEncoder replaces spaces with "+". we need to manually encode "+" to "%20"".
            urlEncodedJavascript = urlEncodedJavascript.replace("+", "%20");
            // rebuild the string
            queryParams.put(MessagingConstants.QueryParameters.JAVASCRIPT_QUERY_KEY, urlEncodedJavascript);
            int count = 0;
            for (final Map.Entry entry : queryParams.entrySet()) {
                if (count == 0) {
                    processedUrlStringBuilder.append("?").append(entry.getKey()).append("=").append(entry.getValue());
                } else {
                    processedUrlStringBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
                }
                count++;
            }
        } catch (final UnsupportedEncodingException unsupportedEncodingException) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid encoding type (%s), javascript will be ignored.", StandardCharsets.UTF_8);
        }
        return processedUrlStringBuilder.toString();
    }

    private static Map<String, String> extractQueryParameters(final String queryString) {
        if (StringUtils.isNullOrEmpty(queryString)) {
            return null;
        }

        final Map<String, String> parameters = new HashMap<String, String>();
        final String[] paramArray = queryString.split("&");

        for (String currentParam : paramArray) {
            // quick out in case this entry is null or empty string
            if (StringUtils.isNullOrEmpty(currentParam)) {
                continue;
            }

            final String[] currentParamArray = currentParam.split("=", 2);

            if (currentParamArray.length != 2 ||
                    (currentParamArray[0].isEmpty() || currentParamArray[1].isEmpty())) {
                continue;
            }

            final String key = currentParamArray[0];
            final String value = currentParamArray[1];
            parameters.put(key, value);
        }

        return parameters;
    }
}
