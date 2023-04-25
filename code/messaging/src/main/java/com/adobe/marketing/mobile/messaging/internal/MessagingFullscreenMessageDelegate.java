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
import java.net.URLDecoder;
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

        final URI uri;

        try {
            uri = new URI(urlString);
        } catch (final URISyntaxException ex) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message URI found (%s), exception is: %s.", urlString, ex.getMessage());
            return true;
        }


        final String messageScheme = uri.getScheme();

        // Quick bail out if scheme is not "adbinapp"
        if (messageScheme == null || !messageScheme.equals(MessagingConstants.QueryParameters.ADOBE_INAPP)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message scheme found in URI. (%s)", urlString);
            return false;
        }

        final MessageSettings messageSettings = fullscreenMessage.getMessageSettings();
        final Message message = (Message) messageSettings.getParent();

        // Handle query parameters
        final String decodedQueryString = uri.getQuery();
        final Map<String, String> messageData = extractQueryParameters(decodedQueryString);
        if (!MapUtils.isNullOrEmpty(messageData)) {
            // handle optional tracking
            final String interaction = messageData.remove(MessagingConstants.QueryParameters.INTERACTION);
            if (!StringUtils.isNullOrEmpty(interaction)) {

                // ensure we have the MessagingExtension class available for tracking
                final Object messagingExtension = message.getParent();
                if (messagingExtension != null) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Tracking message interaction (%s)", interaction);
                    message.track(interaction, MessagingEdgeEventType.IN_APP_INTERACT);
                }
            }

            // handle optional deep link
            String link = messageData.remove(MessagingConstants.QueryParameters.LINK);
            if (!StringUtils.isNullOrEmpty(link)) {

                // handle optional javascript code to be executed
                if (link.startsWith(MessagingConstants.QueryParameters.JAVASCRIPT_QUERY_KEY)) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Evaluating javascript (%s)", link);
                    message.evaluateJavascript(link);
                } else {

                    // if we have any remaining query parameters we need to append them to the deeplink
                    if (!messageData.isEmpty()) {
                        for (final Map.Entry<String, String> entry : messageData.entrySet()) {
                            link = link.concat("&").concat(entry.getKey()).concat("=").concat(entry.getValue());
                        }
                    }
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Loading deeplink (%s)", link);
                    openUrl(link);
                }
            }
        }

        final String host = uri.getHost();
        if (host.equals(MessagingConstants.QueryParameters.PATH_DISMISS)) {
            message.dismiss(true);
        }

        return true;
    }

    // ============================================================================================
    // FullscreenMessageDelegate implementation helper functions
    // ============================================================================================

    /**
     * Open the passed in url using the {@link UIService}.
     *
     * @param url {@link String} containing the deeplink or url to be loaded
     */
    void openUrl(final String url) {
        if (StringUtils.isNullOrEmpty(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not openURL, url is null or empty.");
            return;
        }

        // pass the url to the ui service
        final UIService uiService = ServiceProvider.getInstance().getUIService();
        if (uiService == null || !uiService.showUrl(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Could not open URL (%s)", url);
        }
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
