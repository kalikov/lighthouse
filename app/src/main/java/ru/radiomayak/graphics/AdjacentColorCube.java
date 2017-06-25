package ru.radiomayak.graphics;

class AdjacentColorCube {
    static final int DIMENSION = 32;

    private final int[] frequency = new int[DIMENSION * DIMENSION * DIMENSION];

    int[] getFrequency() {
        return frequency;
    }

    void inc(int r, int g, int b) {
        int x = r % DIMENSION;
        int y = g % DIMENSION;
        int z = b % DIMENSION;
        int index = DIMENSION * DIMENSION * x + DIMENSION * y + z;
        frequency[index]++;
    }
}
