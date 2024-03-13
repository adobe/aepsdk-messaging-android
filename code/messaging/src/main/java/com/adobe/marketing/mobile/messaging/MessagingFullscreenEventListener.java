/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messaging;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventListener;
import com.adobe.marketing.mobile.services.uri.UriOpening;
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
 * This class is the Messaging extension implementation of {@link InAppMessageEventListener}.
 */
class MessagingFullscreenEventListener implements InAppMessageEventListener {
    static final String INTERACTION_BACK_PRESS = "backPress";
    private final static String SELF_TAG = "MessagingFullscreenMessageDelegate";

    /**
     * Invoked when the in-app message is displayed.
     *
     * @param fullscreenMessage the {@link Presentable < InAppMessage >} being displayed
     */
    @Override
    public void onShow(@NonNull final Presentable<InAppMessage> fullscreenMessage) {
        PresentableMessageMapper.InternalMessage message = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().getMessageFromPresentableId(fullscreenMessage.getPresentation().getId());
        if (message != null) {
            if (message.getAutoTrack()) {
                message.track(null, MessagingEdgeEventType.DISPLAY);
            }
            message.recordEventHistory(null, MessagingEdgeEventType.DISPLAY);
        }
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message shown.");
    }

    /**
     * Invoked when the in-app message is dismissed.
     *
     * @param fullscreenMessage the {@link Presentable<InAppMessage>} being dismissed
     */
    @Override
    public void onDismiss(@NonNull final Presentable<InAppMessage> fullscreenMessage) {
        PresentableMessageMapper.InternalMessage message = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().getMessageFromPresentableId(fullscreenMessage.getPresentation().getId());
        if (message != null) {
            if (message.getAutoTrack()) {
                message.track(null, MessagingEdgeEventType.DISMISS);
            }
            message.recordEventHistory(null, MessagingEdgeEventType.DISMISS);
        }
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message dismissed.");
    }

    /**
     * Invoked when the in-app message failed to be displayed.
     */
    @Override
    public void onError(@NonNull final Presentable<InAppMessage> presentable, @NonNull final PresentationError presentationError) {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message failed to show.");
    }

    /**
     * Invoked when a {@link Presentable<InAppMessage>} is attempting to load a URL.
     *
     * @param fullscreenMessage the {@link Presentable<InAppMessage>} instance
     * @param urlString         {@link String} containing the URL being loaded by the {@code AEPMessage}
     * @return true if the SDK wants to handle the URL
     */
    @SuppressWarnings("NestedIfDepth")
    @Override
    public boolean onUrlLoading(@NonNull final Presentable<InAppMessage> fullscreenMessage, @NonNull final String urlString) {
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

        // check adbinapp scheme
        final String messageScheme = uri.getScheme();

        if (!MessagingConstants.QueryParameters.ADOBE_INAPP.equals(messageScheme)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message scheme found in URI. (%s)", urlString);
            return false;
        }

        // url decode the query parameters if present
        final String queryParams;
        try {
            queryParams = URLDecoder.decode(uri.getQuery(), StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException|NullPointerException exception) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG,  "UnsupportedEncodingException occurred when decoding query parameters %s.", uri.getQuery());
            return false;
        }

        // Populate message data
        final Map<String, String> messageData = extractQueryParameters(queryParams);

        final PresentableMessageMapper.InternalMessage message = (PresentableMessageMapper.InternalMessage) PresentableMessageMapper.getInstance().getMessageFromPresentableId(fullscreenMessage.getPresentation().getId());
        if (!MapUtils.isNullOrEmpty(messageData)) {
            // handle optional tracking
            final String interaction = messageData.remove(MessagingConstants.QueryParameters.INTERACTION);
            if (message != null && !StringUtils.isNullOrEmpty(interaction)) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Tracking message interaction (%s)", interaction);
                        message.track(interaction, MessagingEdgeEventType.INTERACT);
                        message.recordEventHistory(interaction, MessagingEdgeEventType.INTERACT);
                    }

            // handle optional deep link
            String link = messageData.remove(MessagingConstants.QueryParameters.LINK);
            if (!StringUtils.isNullOrEmpty(link)) {
                // handle optional javascript code to be executed
                if (link.startsWith(MessagingConstants.QueryParameters.JAVASCRIPT_QUERY_KEY)) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Evaluating javascript (%s)", link);
                    fullscreenMessage.getPresentation().getEventHandler().evaluateJavascript(link, s -> {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Javascript evaluation completed with result: %s", s);
                    });
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
        if ((host.equals(MessagingConstants.QueryParameters.PATH_DISMISS)) || (host.equals(MessagingConstants.QueryParameters.PATH_CANCEL))) {
            if (message != null) {
                message.dismiss();
            }
        }

        return true;
    }

    @Override
    public void onHide(@NonNull final Presentable<InAppMessage> presentable) {
        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Fullscreen message hidden.");
    }

    @Override
    public void onBackPressed(@NonNull final Presentable<InAppMessage> fullscreenMessage) {
        final PresentableMessageMapper.InternalMessage message = (PresentableMessageMapper.InternalMessage) MessagingUtils.getMessageForPresentable(fullscreenMessage);
        if (message != null) {
            if (message.getAutoTrack()) {
                message.track(INTERACTION_BACK_PRESS, MessagingEdgeEventType.INTERACT);
            }
            message.recordEventHistory(INTERACTION_BACK_PRESS, MessagingEdgeEventType.INTERACT);
        }
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
        final UriOpening uriService = ServiceProvider.getInstance().getUriService();
        if (uriService == null || !uriService.openUri(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Could not open URL (%s)", url);
        }
    }

    private Map<String, String> extractQueryParameters(final String queryString) {
        if (StringUtils.isNullOrEmpty(queryString)) {
            return null;
        }

        final Map<String, String> parameters = new HashMap<>();
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
