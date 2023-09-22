package com.adobe.marketing.mobile.messaging;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;

public class MessagingPushTrackerActivity extends Activity {

    private static final String SELF_TAG = "MessagingPushTrackerActivity";

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent == null) {
            Log.warning(MessagingPushConstants.LOG_TAG, SELF_TAG, "Intent is null. Ignoring to track or take action on push notification interaction.");
            finish();
            return;
        }
        final String action = intent.getAction();
        if (StringUtils.isNullOrEmpty(action)) {
            Log.warning(MessagingPushConstants.LOG_TAG, SELF_TAG, "Intent action is null or empty. Ignoring to track or take action on push notification interaction.");
            finish();
            return;
        }

        switch (action) {
            case MessagingPushConstants.NotificationAction.OPENED:
                handlePushOpen(intent);
                break;
            case MessagingPushConstants.NotificationAction.BUTTON_CLICKED:
                handlePushButtonClicked(intent);
                break;
            case MessagingPushConstants.NotificationAction.DISMISSED:
                handlePushDismiss(intent);
                break;
            default:
                break;
        }
        finish();
    }


    /**
     * Handles the push notification open action.
     *
     * @param intent the intent received from the push notification interaction
     */
    private void handlePushOpen(final Intent intent) {
        Messaging.handleNotificationResponse(intent, true, null);
        executePushAction(intent);
    }

    /**
     * Handles clicks on push notification custom buttons.
     *
     * @param intent the intent received from interacting with buttons on push notification
     */
    private void handlePushButtonClicked(final Intent intent) {
        final String actionId = intent.getStringExtra(MessagingPushConstants.Tracking.Keys.ACTION_ID);
        Messaging.handleNotificationResponse(intent, true, actionId);

        // Dismiss the notification once interacted
        final String messageId = intent.getStringExtra(MessagingPushConstants.Tracking.Keys.MESSAGE_ID);
        if(!StringUtils.isNullOrEmpty(messageId)) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(messageId.hashCode());
        } else {
            Log.warning(MessagingPushConstants.LOG_TAG, SELF_TAG, "Message ID is null or empty. Unable to dismiss the notification.");
        }

        executePushAction(intent);
    }

    /**
     * Handles the push notification dismiss action
     *
     * @param intent the intent received from the push notification
     */
    private void handlePushDismiss(final Intent intent) {
        Messaging.handleNotificationResponse(intent, false, null);
    }

    /**
     * Reads the URI and executes the action that is configured for the push notification.
     *
     * If no URI is configured, by default the application will be opened.
     *
     * @param intent the intent received from the push notification
     */
    private void executePushAction(final Intent intent) {
        final String actionUri = intent.getStringExtra(MessagingPushConstants.Tracking.Keys.ACTION_URI);
        if (StringUtils.isNullOrEmpty(actionUri)) {
            openApplication();
        } else {
            openUri(actionUri);
        }
    }

    /**
     * Use this method to create an intent to open the application.
     * If the application is already open and in the foreground, the action will resume the current activity.
     */
    private void openApplication() {
        final Activity currentActivity = ServiceProvider.getInstance().getAppContextService().getCurrentActivity();
        final Intent launchIntent;
        if (currentActivity != null) {
            launchIntent = new Intent(currentActivity, currentActivity.getClass());
        } else {
            Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "There is no active activity. Starting the launcher Activity.");
            launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        }
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        } else {
            Log.warning(MessagingPushConstants.LOG_TAG, SELF_TAG, "Unable to create an intent to open the application from the notification interaction.");
        }
    }

    /**
     * Use this method to create an intent to open the the provided URI.
     *
     * @param uri the uri to open
     */
    private void openUri(final String uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        stackBuilder.startActivities();
    }
}
