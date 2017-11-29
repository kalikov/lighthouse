package ru.radiomayak.media;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;

public class ByteMapTest {
    @Test
    public void testSingleSegmentContains() {
        ByteMap byteMap = new ByteMap(100, 1000);
        Assert.assertTrue(byteMap.contains(100, 1000));
        Assert.assertTrue(byteMap.contains(101, 999));
        Assert.assertFalse(byteMap.contains(99, 1000));
        Assert.assertFalse(byteMap.contains(100, 1001));
        Assert.assertFalse(byteMap.contains(0, 500));
        Assert.assertFalse(byteMap.contains(500, 2000));
        Assert.assertFalse(byteMap.contains(10, 50));
        Assert.assertFalse(byteMap.contains(2000, 0));
        Assert.assertFalse(byteMap.contains(100, 0));
    }

    @Test
    public void testSingleSegmentSize() {
        ByteMap byteMap = new ByteMap(100, 1000);
        Assert.assertEquals(901, byteMap.size());
    }

    @Test
    public void testEmptyMerge() {
        ByteMap byteMap = new ByteMap(0);
        Assert.assertEquals(0, byteMap.merge(100, 1000));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{100, 1000}));
    }

    @Test
    public void testInclusiveMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 1000});
        Assert.assertEquals(-1, byteMap.merge(200, 500));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{100, 1000}));
    }

    @Test
    public void testSingleNonIntersectingLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 1000});
        Assert.assertEquals(0, byteMap.merge(10, 50));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{10, 50, 100, 1000}));
    }

    @Test
    public void testSingleNonIntersectingUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 1000});
        Assert.assertEquals(0, byteMap.merge(2000, 2100));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{100, 1000, 2000, 2100}));
    }

    @Test
    public void testMultipleNonIntersectingLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(0, byteMap.merge(0, 10));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{0, 10, 20, 30, 40, 50, 60, 70}));
    }

    @Test
    public void testMultipleNonIntersectingUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(0, byteMap.merge(80, 90));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 40, 50, 60, 70, 80, 90}));
    }

    @Test
    public void testMultipleNonIntersectingMiddleMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 60, 70, 80, 90});
        Assert.assertEquals(0, byteMap.merge(40, 50));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 40, 50, 60, 70, 80, 90}));
    }

    @Test
    public void testSingleIntersectingLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 200});
        Assert.assertEquals(51, byteMap.merge(0, 150));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{0, 200}));
    }

    @Test
    public void testSingleIntersectingUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 200});
        Assert.assertEquals(51, byteMap.merge(150, 300));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{100, 300}));
    }

    @Test
    public void testSingleIntersectingContainingMerge() {
        ByteMap byteMap = new ByteMap(new int[]{100, 200});
        Assert.assertEquals(101, byteMap.merge(50, 300));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{50, 300}));
    }

    @Test
    public void testMultipleOneIntersectingLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(11, byteMap.merge(55, 70));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 40, 50, 55, 70}));
    }

    @Test
    public void testMultipleOneIntersectingUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(6, byteMap.merge(45, 55));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 40, 55, 60, 70}));
    }

    @Test
    public void testMultipleOneIntersectingContainingMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(11, byteMap.merge(35, 55));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 35, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(22, byteMap.merge(10, 55));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{10, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerMiddleMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(28, byteMap.merge(10, 65));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{10, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(33, byteMap.merge(10, 80));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{10, 80}));
    }

    @Test
    public void testMultipleIntersectingMiddleLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, byteMap.merge(25, 55));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingMiddleMiddleMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(23, byteMap.merge(25, 65));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 70}));
    }

    @Test
    public void testMultipleIntersectingMiddleUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, byteMap.merge(45, 80));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 40, 80}));
    }

    @Test
    public void testMultipleIntersectingUpperLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(22, byteMap.merge(35, 75));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 35, 75, 80, 90}));
    }

    @Test
    public void testMultipleIntersectingUpperMiddleMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, byteMap.merge(35, 65));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 35, 70}));
    }

    @Test
    public void testMultipleIntersectingUpperUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(22, byteMap.merge(35, 80));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 30, 35, 80}));
    }

    @Test
    public void testComplementSingleIntersectingLowerMerge() {
        ByteMap byteMap = new ByteMap(new int[]{60, 80});
        Assert.assertEquals(0, byteMap.merge(41, 59));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{41, 80}));
    }

    @Test
    public void testComplementSingleIntersectingUpperMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 40});
        Assert.assertEquals(0, byteMap.merge(41, 59));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 59}));
    }

    @Test
    public void testComplementMultipleIntersectingMerge() {
        ByteMap byteMap = new ByteMap(new int[]{20, 40, 60, 80});
        Assert.assertEquals(0, byteMap.merge(41, 59));
        Assert.assertTrue(Arrays.toString(byteMap.segments()), Arrays.equals(byteMap.segments(), new int[]{20, 80}));
    }

    @Test
    public void testMultipleSegmentsContains() {
        ByteMap byteMap = new ByteMap(new int[]{100, 150, 200, 2000, 3000, 4000});
        Assert.assertTrue(byteMap.contains(100, 150));
        Assert.assertTrue(byteMap.contains(101, 149));
        Assert.assertFalse(byteMap.contains(99, 150));
        Assert.assertFalse(byteMap.contains(100, 151));
        Assert.assertFalse(byteMap.contains(0, 120));
        Assert.assertFalse(byteMap.contains(120, 200));
        Assert.assertFalse(byteMap.contains(10, 50));
        Assert.assertFalse(byteMap.contains(200, 0));
        Assert.assertFalse(byteMap.contains(100, 0));
        Assert.assertTrue(byteMap.contains(200, 2000));
        Assert.assertTrue(byteMap.contains(3000, 4000));
        Assert.assertFalse(byteMap.contains(2999, 4000));
        Assert.assertFalse(byteMap.contains(2000, 3000));
    }

    @Test
    public void testMultipleSegmentsSize() {
        ByteMap byteMap = new ByteMap(new int[]{100, 150, 200, 2000, 3000, 4000});
        Assert.assertEquals(2853, byteMap.size());
    }

    @Test
    public void testOffsetBelow() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(0, byteMap.toOffset(10));
    }

    @Test
    public void testOffsetFirstSegmentStart() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(0, byteMap.toOffset(20));
    }

    @Test
    public void testOffsetFirstSegmentMiddle() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(5, byteMap.toOffset(25));
    }

    @Test
    public void testOffsetFirstSegmentEnd() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(10, byteMap.toOffset(30));
    }

    @Test
    public void testOffsetBelowSecondSegment() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(11, byteMap.toOffset(35));
    }

    @Test
    public void testOffsetSecondSegmentStart() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(11, byteMap.toOffset(40));
    }

    @Test
    public void testOffsetSecondSegmentMiddle() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(16, byteMap.toOffset(45));
    }

    @Test
    public void testOffsetSecondSegmentEnd() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(21, byteMap.toOffset(50));
    }

    @Test
    public void testOffsetLastSegmentEnd() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(43, byteMap.toOffset(90));
    }

    @Test
    public void testOffsetAboveLastSegment() {
        ByteMap byteMap = new ByteMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(44, byteMap.toOffset(100));
    }
}
