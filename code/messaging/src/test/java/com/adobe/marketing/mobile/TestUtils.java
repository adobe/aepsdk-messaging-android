package com.adobe.marketing.mobile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestUtils {
    private static final String LOG_TAG = "TestUtils";
    private static final int STREAM_WRITE_BUFFER_SIZE = 4096;
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

    static boolean writeInputStreamIntoFile(final File cachedFile, final InputStream inputStream, final boolean append) {
        if (cachedFile == null || inputStream == null) {
            Log.error(LOG_TAG, "%s - Failed to write InputStream to the cache. The cachedFile or inputStream is null.");
            return false;
        }

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }
            outputStream.flush();
        } catch (final IOException e) {
            Log.error(LOG_TAG, "%s - IOException while attempting to write remote file (%s)", e);
            return false;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (final IOException e) {
                Log.error(LOG_TAG, "%s - Unable to close the OutputStream (%s) ", e);
            }
        }
        return true;
    }
}
