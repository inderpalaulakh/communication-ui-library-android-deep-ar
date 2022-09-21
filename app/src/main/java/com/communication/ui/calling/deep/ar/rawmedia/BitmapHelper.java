package com.communication.ui.calling.deep.ar.rawmedia;


import android.graphics.Bitmap;
import android.graphics.Rect;

public class BitmapHelper {
    public static Bitmap resizeBitmap(Bitmap bitmap, int reqWidth, int reqHeight) {
        RequestSizeOptions options = RequestSizeOptions.RESIZE_FIT;
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == RequestSizeOptions.RESIZE_FIT ||
                    options == RequestSizeOptions.RESIZE_INSIDE ||
                    options == RequestSizeOptions.RESIZE_EXACT || options == RequestSizeOptions.RESIZE_CENTRE_CROP)) {

                Bitmap resized = null;
                if (options == RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
                } else {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale), false);
                    }
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_CENTRE_CROP) {
                        int smaller_side = (height - width) > 0 ? width : height;
                        int half_smaller_side = smaller_side / 2;
                        Rect initialRect = new Rect(0, 0, width, height);
                        Rect finalRect = new Rect(initialRect.centerX() - half_smaller_side, initialRect.centerY() - half_smaller_side,
                                initialRect.centerX() + half_smaller_side, initialRect.centerY() + half_smaller_side);
                        bitmap = Bitmap.createBitmap(bitmap, finalRect.left, finalRect.top, finalRect.width(), finalRect.height(), null, true);
                        //keep in mind we have square as request for cropping, otherwise - it is useless
                        resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
                    }

                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle();
                    }
                    return resized;
                }
            }
        } catch (Exception e) {
        }
        return bitmap;
    }

    enum RequestSizeOptions {
        RESIZE_FIT,
        RESIZE_INSIDE,
        RESIZE_EXACT,
        RESIZE_CENTRE_CROP
    }

}
