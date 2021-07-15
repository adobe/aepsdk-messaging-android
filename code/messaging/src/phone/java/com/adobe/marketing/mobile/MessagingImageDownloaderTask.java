package com.adobe.marketing.mobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

class MessagingImageDownloaderTask implements Callable<Bitmap> {
    private final String url;

    MessagingImageDownloaderTask(String url) {
        this.url = url;
    }

    @Override
    public Bitmap call() {
        return download(url);
    }

    private Bitmap download(String url) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        try {
            URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            bitmap = BitmapFactory.decodeStream(connection.getInputStream());
        } catch (IOException e) {
            Log.w(MessagingConstant.LOG_TAG, "Error downloading the image. Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }
}
