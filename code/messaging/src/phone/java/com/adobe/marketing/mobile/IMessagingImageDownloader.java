package com.adobe.marketing.mobile;

import android.content.Context;
import android.graphics.Bitmap;

public interface IMessagingImageDownloader {
    Bitmap getBitmapFromUrl(Context context, String imageUrl);
}
