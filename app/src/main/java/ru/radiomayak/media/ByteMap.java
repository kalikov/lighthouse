package ru.radiomayak.media;

public class ByteMap {
    private static final int[] EMPTY_SEGMENTS = new int[0];

    private int capacity;
    private int[] segments;

    ByteMap(int capacity) {
        this.capacity = capacity;
        segments = EMPTY_SEGMENTS;
    }

    ByteMap(int[] segments) {
        this(0, segments);
    }

    ByteMap(int capacity, int[] segments) {
        if (segments == null || segments.length % 2 == 1) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < segments.length; i += 2) {
            if (segments[i] < 0 || segments[i + 1] <= 0 || segments[i] > segments[i + 1]) {
                throw new IllegalArgumentException();
            }
            if (i > 0 && segments[i] <= segments[i - 1]) {
                throw new IllegalArgumentException();
            }
        }
        this.capacity = capacity;
        this.segments = segments;
    }

    ByteMap(int from, int to) {
        this(0, from, to);
    }

    ByteMap(int capacity, int from, int to) {
        if (from < 0 || to <= 0 || from > to) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.segments = new int[]{from, to};
    }

    boolean contains(int from, int to) {
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (from >= start && from <= end) {
                return to >= from && to <= end;
            }
            if (from < start) {
                break;
            }
        }
        return false;
    }

    int capacity() {
        return capacity;
    }

    void capacity(int capacity) {
        this.capacity = capacity;
    }

    int[] segments() {
        return segments;
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < segments.length; i += 2) {
            size += segments[i + 1] - segments[i] + 1;
        }
        return size;
    }

    int size(int from, int to) {
        int size = 0;
        int overlap = 0;
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            size += end - start + 1;
            if (from >= start && to <= end) {
                overlap = to - from + 1;
            } else if (from <= start && to >= start || from <= end && to >= end) {
                overlap += (end - start + 1) - Math.max(0, from - start) - Math.max(0, end - to);
            }
        }
        return size + to - from + 1 - overlap;
    }

    int merge(int from, int to) {
        if (from < 0 || to <= 0 || from > to) {
            throw new IllegalArgumentException();
        }
        int intersections = 0;
        int overlap = 0;
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (from >= start && to <= end) {
                return -1;
            }
            if (from <= start && to >= start || from <= end && to >= end) {
                intersections++;
                overlap += (end - start + 1) - Math.max(0, from - start) - Math.max(0, end - to);
            }
            if (start >= to) {
                break;
            }
        }

        if (intersections == 0) {
            mergeNoIntersection(from, to);
        } else if (intersections == 1) {
            mergeSingleIntersection(from, to);
        } else {
            mergeMultipleIntersections(intersections, from, to);
        }
        return overlap;
    }

    private void mergeNoIntersection(int from, int to) {
        int[] array = new int[segments.length + 2];
        int pos = 0;
        for (int i = 0, j = 0; i < segments.length; i += 2, j += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (start < from) {
                pos += 2;
            } else if (from < start && i == j) {
                j += 2;
            }
            array[j] = start;
            array[j + 1] = end;
        }
        array[pos] = from;
        array[pos + 1] = to;
        segments = array;
    }

    private void mergeSingleIntersection(int from, int to) {
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (from < start && to >= start) {
                segments[i] = from;
            }
            if (from <= end && to > end) {
                segments[i + 1] = to;
            }
            if (from <= start && to >= start || from <= end && to >= end) {
                break;
            }
        }
    }

    private void mergeMultipleIntersections(int intersections, int from, int to) {
        int[] array = new int[segments.length - 2 * (intersections - 1)];
        for (int i = 0, j = 0; j < array.length; j += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (from <= start && to >= start || from <= end && to > end) {
                array[j] = Math.min(from, start);
                while (from <= start && to >= start || from <= end && to > end) {
                    array[j + 1] = Math.max(to, end);
                    i += 2;
                    if (i >= segments.length) {
                        break;
                    }
                    start = segments[i];
                    end = segments[i + 1];
                }
            } else {
                array[j] = start;
                array[j + 1] = end;
                i += 2;
            }
        }
        segments = array;
    }

    int toOffset(int position) {
        int offset = 0;
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            if (position >= start) {
                offset += Math.min(position, end + 1) - start;
            } else {
                break;
            }
        }
        return offset;
    }

    boolean isPartial() {
        if (capacity == 0) {
            return true;
        }
        int size = 0;
        for (int i = 0; i < segments.length; i += 2) {
            int start = segments[i];
            int end = segments[i + 1];
            size += end - start + 1;
        }
        return size < capacity;
    }
}
