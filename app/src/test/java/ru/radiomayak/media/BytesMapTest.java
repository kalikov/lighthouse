package ru.radiomayak.media;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;

public class BytesMapTest {
    @Test
    public void testSingleSegmentContains() {
        BytesMap bytesMap = new BytesMap(100, 1000);
        Assert.assertTrue(bytesMap.contains(100, 1000));
        Assert.assertTrue(bytesMap.contains(101, 999));
        Assert.assertFalse(bytesMap.contains(99, 1000));
        Assert.assertFalse(bytesMap.contains(100, 1001));
        Assert.assertFalse(bytesMap.contains(0, 500));
        Assert.assertFalse(bytesMap.contains(500, 2000));
        Assert.assertFalse(bytesMap.contains(10, 50));
        Assert.assertFalse(bytesMap.contains(2000, 0));
        Assert.assertFalse(bytesMap.contains(100, 0));
    }

    @Test
    public void testSingleSegmentSize() {
        BytesMap bytesMap = new BytesMap(100, 1000);
        Assert.assertEquals(901, bytesMap.size());
    }

    @Test
    public void testEmptyMerge() {
        BytesMap bytesMap = new BytesMap(0);
        Assert.assertEquals(0, bytesMap.merge(100, 1000));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{100, 1000}));
    }

    @Test
    public void testInclusiveMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 1000});
        Assert.assertEquals(-1, bytesMap.merge(200, 500));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{100, 1000}));
    }

    @Test
    public void testSingleNonIntersectingLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 1000});
        Assert.assertEquals(0, bytesMap.merge(10, 50));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{10, 50, 100, 1000}));
    }

    @Test
    public void testSingleNonIntersectingUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 1000});
        Assert.assertEquals(0, bytesMap.merge(2000, 2100));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{100, 1000, 2000, 2100}));
    }

    @Test
    public void testMultipleNonIntersectingLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(0, bytesMap.merge(0, 10));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{0, 10, 20, 30, 40, 50, 60, 70}));
    }

    @Test
    public void testMultipleNonIntersectingUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(0, bytesMap.merge(80, 90));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 40, 50, 60, 70, 80, 90}));
    }

    @Test
    public void testMultipleNonIntersectingMiddleMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 60, 70, 80, 90});
        Assert.assertEquals(0, bytesMap.merge(40, 50));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 40, 50, 60, 70, 80, 90}));
    }

    @Test
    public void testSingleIntersectingLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 200});
        Assert.assertEquals(51, bytesMap.merge(0, 150));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{0, 200}));
    }

    @Test
    public void testSingleIntersectingUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 200});
        Assert.assertEquals(51, bytesMap.merge(150, 300));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{100, 300}));
    }

    @Test
    public void testSingleIntersectingContainingMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 200});
        Assert.assertEquals(101, bytesMap.merge(50, 300));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{50, 300}));
    }

    @Test
    public void testMultipleOneIntersectingLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(11, bytesMap.merge(55, 70));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 40, 50, 55, 70}));
    }

    @Test
    public void testMultipleOneIntersectingUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(6, bytesMap.merge(45, 55));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 40, 55, 60, 70}));
    }

    @Test
    public void testMultipleOneIntersectingContainingMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(11, bytesMap.merge(35, 55));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 35, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(22, bytesMap.merge(10, 55));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{10, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerMiddleMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(28, bytesMap.merge(10, 65));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{10, 70}));
    }

    @Test
    public void testMultipleIntersectingLowerUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(33, bytesMap.merge(10, 80));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{10, 80}));
    }

    @Test
    public void testMultipleIntersectingMiddleLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, bytesMap.merge(25, 55));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 55, 60, 70}));
    }

    @Test
    public void testMultipleIntersectingMiddleMiddleMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(23, bytesMap.merge(25, 65));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 70}));
    }

    @Test
    public void testMultipleIntersectingMiddleUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, bytesMap.merge(45, 80));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 40, 80}));
    }

    @Test
    public void testMultipleIntersectingUpperLowerMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(22, bytesMap.merge(35, 75));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 35, 75, 80, 90}));
    }

    @Test
    public void testMultipleIntersectingUpperMiddleMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(17, bytesMap.merge(35, 65));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 35, 70}));
    }

    @Test
    public void testMultipleIntersectingUpperUpperMerge() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70});
        Assert.assertEquals(22, bytesMap.merge(35, 80));
        Assert.assertTrue(Arrays.toString(bytesMap.segments()), Arrays.equals(bytesMap.segments(), new int[]{20, 30, 35, 80}));
    }

    @Test
    public void testMultipleSegmentsContains() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 150, 200, 2000, 3000, 4000});
        Assert.assertTrue(bytesMap.contains(100, 150));
        Assert.assertTrue(bytesMap.contains(101, 149));
        Assert.assertFalse(bytesMap.contains(99, 150));
        Assert.assertFalse(bytesMap.contains(100, 151));
        Assert.assertFalse(bytesMap.contains(0, 120));
        Assert.assertFalse(bytesMap.contains(120, 200));
        Assert.assertFalse(bytesMap.contains(10, 50));
        Assert.assertFalse(bytesMap.contains(200, 0));
        Assert.assertFalse(bytesMap.contains(100, 0));
        Assert.assertTrue(bytesMap.contains(200, 2000));
        Assert.assertTrue(bytesMap.contains(3000, 4000));
        Assert.assertFalse(bytesMap.contains(2999, 4000));
        Assert.assertFalse(bytesMap.contains(2000, 3000));
    }

    @Test
    public void testMultipleSegmentsSize() {
        BytesMap bytesMap = new BytesMap(new int[]{100, 150, 200, 2000, 3000, 4000});
        Assert.assertEquals(2853, bytesMap.size());
    }

    @Test
    public void testOffsetBelow() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(0, bytesMap.toOffset(10));
    }

    @Test
    public void testOffsetFirstSegmentStart() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(0, bytesMap.toOffset(20));
    }

    @Test
    public void testOffsetFirstSegmentMiddle() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(5, bytesMap.toOffset(25));
    }

    @Test
    public void testOffsetFirstSegmentEnd() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(10, bytesMap.toOffset(30));
    }

    @Test
    public void testOffsetBelowSecondSegment() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(11, bytesMap.toOffset(35));
    }

    @Test
    public void testOffsetSecondSegmentStart() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(11, bytesMap.toOffset(40));
    }

    @Test
    public void testOffsetSecondSegmentMiddle() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(16, bytesMap.toOffset(45));
    }

    @Test
    public void testOffsetSecondSegmentEnd() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(21, bytesMap.toOffset(50));
    }

    @Test
    public void testOffsetLastSegmentEnd() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(43, bytesMap.toOffset(90));
    }

    @Test
    public void testOffsetAboveLastSegment() {
        BytesMap bytesMap = new BytesMap(new int[]{20, 30, 40, 50, 60, 70, 80, 90});
        Assert.assertEquals(44, bytesMap.toOffset(100));
    }
}
