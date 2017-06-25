package ru.radiomayak.podcasts;

import android.graphics.Bitmap;

import ru.radiomayak.graphics.BitmapColors;

class BitmapInfo {
    private final Bitmap bitmap;
    private final int primaryColor;
    private final int secondaryColor;

    BitmapInfo(Bitmap bitmap) {
        this.bitmap = bitmap;
        BitmapColors colorCube = getBitmapColorCube(bitmap);
        primaryColor = colorCube.getPrimaryColorByCube();
        secondaryColor = colorCube.getMaxContrastingColor(primaryColor);
    }

    BitmapInfo(Bitmap bitmap, int primaryColor, int secondaryColor) {
        this.bitmap = bitmap;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    private static BitmapColors getBitmapColorCube(Bitmap bitmap) {
        BitmapColors cube = new BitmapColors();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w];
        for (int i = 0; i < h; i++) {
            bitmap.getPixels(pixels, 0, w, 0, i, w, 1);
            for (int pixel : pixels) {
                cube.inc(pixel);
            }
        }
        return cube;
    }

    Bitmap getBitmap() {
        return bitmap;
    }

    int getPrimaryColor() {
        return primaryColor;
    }

    int getSecondaryColor() {
        return secondaryColor;
    }
}
