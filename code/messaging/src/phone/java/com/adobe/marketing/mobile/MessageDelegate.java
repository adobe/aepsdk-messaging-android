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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

public class MessageDelegate implements UIService.FullscreenMessageDelegate {
    private final static String SELF_TAG = "MessageDelegate";
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, MessagingConstants.TrackingKeys.IAM.EventType.INTERACT);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, interactionType);

        final Event event = new Event.Builder(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_EDGE_EVENT, MessagingConstants.EventType.MESSAGING, MessagingConstants.EventSource.REQUEST_CONTENT).setEventData(eventData).build();
        Log.debug(LOG_TAG, "%s - Tracking interaction (%s) for message id %s", SELF_TAG, interactionType, messageId);
        messagingInternal.handleTrackingInfo(event);
    }

    /**
     * Requests that the {@code UIService} show this {@code url}.
     *
     * @param url {@link String} containing url to be shown
     */
    protected void openUrl(final String url) {
        if (StringUtils.isNullOrEmpty(url)) {
            Log.debug(LOG_TAG, "%s - Cannot open a null or empty URL.", SELF_TAG);
            return;
        }

        final UIService uiService = MessagingUtils.getUIService();

        if (uiService == null || !uiService.showUrl(url)) {
            Log.debug(LOG_TAG, "%s - Could not open URL (%s)", SELF_TAG, url);
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
        final Message message = (Message) fullscreenMessage.getParent();
        message.dismiss();
    }

    @Override
    public boolean shouldShowMessage(final UIService.FullscreenMessage fullscreenMessage) {
        return true;
    }

    @Override
    public boolean overrideUrlLoad(final UIService.FullscreenMessage fullscreenMessage, final String urlString) {
        Log.trace(LOG_TAG, "%s - Fullscreen overrideUrlLoad callback received with url (%s)", SELF_TAG, urlString);

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(LOG_TAG, "%s - Cannot process provided URL string, it is null or empty.", SELF_TAG);
            return true;
        }

        URI uri;
        try {
            uri = new URI(urlString);

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

        final Message message = (Message) fullscreenMessage.getParent();
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

            final String host = uri.getHost();
            if (host.equals(MessagingConstants.MESSAGING_SCHEME.PATH_DISMISS)) {
                message.dismiss();
            }

            final String deeplink = messageData.get("link");
            if (!StringUtils.isNullOrEmpty(deeplink)) {
                openUrl(deeplink);
            }
        }

        return true;
    }

    @Override
    public void onShowFailure() {
        Log.debug(LOG_TAG,
                "%s - Fullscreen message failed to show.", SELF_TAG);
    }
}
