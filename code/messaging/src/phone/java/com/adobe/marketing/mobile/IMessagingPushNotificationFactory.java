package com.adobe.marketing.mobile;

import android.app.Notification;
import android.content.Context;

import com.adobe.marketing.mobile.MessagingPushPayload;

public interface IMessagingPushNotificationFactory {
    Notification create(Context context, MessagingPushPayload payload, String messageId);
}
