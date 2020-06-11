package com.gpufast.utils;

import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;

public class Util {

    public static void saveRgb2Bitmap(final Buffer buf, final String filename, final int width, final int height) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedOutputStream bos = null;
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(filename));
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buf);
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                    bmp.recycle();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null) {
                        try {
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

}
