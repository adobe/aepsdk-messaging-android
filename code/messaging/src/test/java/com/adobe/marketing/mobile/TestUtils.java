package com.adobe.marketing.mobile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestUtils {
    static void waitForExecutor(final ExecutorService executor, final int executorTime) {
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                // Fake task to check the execution termination
            }
        });

        try {
            future.get(executorTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail(String.format("Executor took longer than %s (sec)", executorTime));
        }
    }

    static JSONArray convertActionButtonListToJsonArray(final List<MessagingPushPayload.ActionButton> list) {
        JSONArray jsonArray = new JSONArray();
        for (MessagingPushPayload.ActionButton button : list) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(MessagingTestConstants.PushNotificationPayload.ActionButtons.LABEL, button.getLabel());
                jsonObject.put(MessagingTestConstants.PushNotificationPayload.ActionButtons.URI, button.getLink());
                jsonObject.put(MessagingTestConstants.PushNotificationPayload.ActionButtons.TYPE, button.getType());
            } catch (JSONException e) {
                e.printStackTrace();
                return jsonArray;
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
}
