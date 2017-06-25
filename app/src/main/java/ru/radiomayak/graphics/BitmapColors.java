package ru.radiomayak.graphics;

import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;

public class BitmapColors {
    private static final int DIMENSION = 256 / AdjacentColorCube.DIMENSION;

    private final AdjacentColorCube[] cubes = new AdjacentColorCube[DIMENSION * DIMENSION * DIMENSION];

    public void inc(int color) {
        inc(Color.red(color), Color.green(color), Color.blue(color));
    }

    public void inc(int r, int g, int b) {
        int x = r / AdjacentColorCube.DIMENSION;
        int y = g / AdjacentColorCube.DIMENSION;
        int z = b / AdjacentColorCube.DIMENSION;
        int index = DIMENSION * DIMENSION * x + DIMENSION * y + z;
        AdjacentColorCube cube = cubes[index];
        if (cube == null) {
            cube = cubes[index] = new AdjacentColorCube();
        }
        cube.inc(r, g, b);
    }

    public int getPrimaryColor() {
        int rank = 0;
        int color = 0;

        int x = 0;
        int y = 0;
        int z = 0;
        for (AdjacentColorCube cube : cubes) {
            if (cube != null) {
                int[] frequency = cube.getFrequency();
                int cubeX = 0;
                int cubeY = 0;
                int cubeZ = 0;
                for (int item : frequency) {
                    if (item > rank) {
                        rank = item;
                        color = Color.rgb(x + cubeX, y + cubeY, z + cubeZ);
                    }
                    cubeZ++;
                    if (cubeZ >= AdjacentColorCube.DIMENSION) {
                        cubeZ = 0;
                        cubeY++;
                        if (cubeY >= AdjacentColorCube.DIMENSION) {
                            cubeY = 0;
                            cubeX++;
                        }
                    }
                }
            }
            z += AdjacentColorCube.DIMENSION;
            if (z >= 256) {
                z = 0;
                y += AdjacentColorCube.DIMENSION;
                if (y >= 256) {
                    y = 0;
                    x += AdjacentColorCube.DIMENSION;
                }
            }
        }
        return color;
    }

    public int getPrimaryColorByCube() {
        int rank = 0;
        int color = 0;

        int x = 0;
        int y = 0;
        int z = 0;
        for (AdjacentColorCube cube : cubes) {
            if (cube != null) {
                int cubeRank = 0;
                int cubeColor = 0;
                int totalRank = 0;
                int[] frequency = cube.getFrequency();
                int cubeX = 0;
                int cubeY = 0;
                int cubeZ = 0;
                for (int item : frequency) {
                    if (item > cubeRank) {
                        cubeRank = item;
                        cubeColor = Color.rgb(x + cubeX, y + cubeY, z + cubeZ);
                    }
                    totalRank += item;
                    cubeZ++;
                    if (cubeZ >= AdjacentColorCube.DIMENSION) {
                        cubeZ = 0;
                        cubeY++;
                        if (cubeY >= AdjacentColorCube.DIMENSION) {
                            cubeY = 0;
                            cubeX++;
                        }
                    }
                }
                if (totalRank > rank) {
                    rank = totalRank;
                    color = cubeColor;
                }
            }
            z += AdjacentColorCube.DIMENSION;
            if (z >= 256) {
                z = 0;
                y += AdjacentColorCube.DIMENSION;
                if (y >= 256) {
                    y = 0;
                    x += AdjacentColorCube.DIMENSION;
                }
            }
        }
        return color;
    }

    public int getMaxContrastingColor(int target) {
        int color = 0;
        double contrast = 0;

        int x = 0;
        int y = 0;
        int z = 0;
        for (AdjacentColorCube cube : cubes) {
            if (cube != null) {
                int[] frequency = cube.getFrequency();
                int cubeX = 0;
                int cubeY = 0;
                int cubeZ = 0;
                for (int item : frequency) {
                    if (item > 0) {
                        int rgb = Color.rgb(x + cubeX, y + cubeY, z + cubeZ);
                        double itemContrast = ColorUtils.calculateContrast(target, rgb);
                        if (itemContrast > contrast) {
                            color = rgb;
                            contrast = itemContrast;
                        }
                    }
                    cubeZ++;
                    if (cubeZ >= AdjacentColorCube.DIMENSION) {
                        cubeZ = 0;
                        cubeY++;
                        if (cubeY >= AdjacentColorCube.DIMENSION) {
                            cubeY = 0;
                            cubeX++;
                        }
                    }
                }
            }
            z += AdjacentColorCube.DIMENSION;
            if (z >= 256) {
                z = 0;
                y += AdjacentColorCube.DIMENSION;
                if (y >= 256) {
                    y = 0;
                    x += AdjacentColorCube.DIMENSION;
                }
            }
        }
        return color;
    }
}
