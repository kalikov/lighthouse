package ru.radiomayak.graphics;

import android.graphics.Bitmap;

import ru.radiomayak.graphics.BitmapColors;

public class BitmapInfo {
    private final Bitmap bitmap;
    private final int primaryColor;
    private final int secondaryColor;

    public BitmapInfo(Bitmap bitmap) {
        this.bitmap = bitmap;
        BitmapColors colorCube = getBitmapColorCube(bitmap);
        primaryColor = colorCube.getPrimaryColorByCube();
        secondaryColor = colorCube.getMaxContrastingColor(primaryColor);
    }

    public BitmapInfo(Bitmap bitmap, int primaryColor, int secondaryColor) {
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

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }
}
