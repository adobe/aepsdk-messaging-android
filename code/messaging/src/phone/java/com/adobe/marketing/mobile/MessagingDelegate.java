package com.adobe.marketing.mobile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

public class MessagingDelegate implements UIService.FullscreenMessageDelegate {
    private boolean lastMessageDisplayed = false;
    private boolean shouldShowMessages = true;

    @Override
    public void onShow(UIService.UIFullScreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "onShow - Fullscreen message shown.");
        lastMessageDisplayed = true;
    }

    @Override
    public void onDismiss(UIService.UIFullScreenMessage fullscreenMessage) {
        Log.debug(LOG_TAG,
                "onDismiss - Fullscreen message dismissed.");
        lastMessageDisplayed = true;
    }

    @Override
    public boolean overrideUrlLoad(UIService.UIFullScreenMessage fullscreenMessage, String urlString) {
        lastMessageDisplayed = true;
        Log.trace(LOG_TAG, "Fullscreen overrideUrlLoad callback received with url (%s)", urlString);

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(LOG_TAG, "Cannot process provided URL string, it is null or empty.");
            return true;
        }

        URI uri = null;

        try {
            uri = new URI(urlString);

        } catch (URISyntaxException ex) {
            Log.debug(LOG_TAG, "overrideUrlLoad -  Invalid message URI found (%s).", urlString);
            return true;
        }

        // check adbinapp scheme
        final String messageScheme = uri.getScheme();

        if (!messageScheme.equals(MessagingConstants.MESSAGING_SCHEME.ADOBE_INAPP)) {
            Log.debug(LOG_TAG, "overrideUrlLoad -  Invalid message scheme found in URI. (%s)", urlString);
            return false;
        }

        // cancel or confirm
        final String host = uri.getHost();

        if (!host.equals(MessagingConstants.MESSAGING_SCHEME.PATH_CONFIRM) &&
                !host.equals(MessagingConstants.MESSAGING_SCHEME.PATH_CANCEL)) {
            Log.debug(LOG_TAG,
                    "overrideUrlLoad -  Unsupported URI host found, neither \"confirm\" nor \"cancel\". (%s)", urlString);
            return false;
        }

        final String query = uri.getQuery();

        // Populate message data
        final Map<String, String> messageData = UrlUtilities.extractQueryParameters(query);

        if (messageData != null && !messageData.isEmpty()) {
            messageData.put(MessagingConstants.MESSAGE_INTERACTION.TYPE, host);

            // TODO: handle message interaction tracking / deeplink url
        }

        if (fullscreenMessage != null) {
            fullscreenMessage.remove();
        }

        return true;
    }

    @Override
    public void onShowFailure() {
        Log.debug(LOG_TAG,
                "onShowFailure - Fullscreen message failed to show.");
        lastMessageDisplayed = false;
    }

    public void setShouldShowMessages(boolean shouldShowMessages) {
        this.shouldShowMessages = shouldShowMessages;
    }

    public boolean getShowMessageStatus() {
        return shouldShowMessages;
    }

    // method added for unit testing
    boolean wasLastMessageDisplayed() {
        return lastMessageDisplayed;
    }
}
