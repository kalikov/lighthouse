package ru.radiomayak.http;

import org.junit.Assert;
import org.junit.Test;

public class HttpRangeTest {
    @Test
    public void testFirstNormalRange() {
        HttpRange range = HttpRange.parseFirst("bytes 100-1000");
        Assert.assertEquals(new HttpRange(100, 1000), range);
    }

    @Test
    public void testFirstNormalRangeLength() {
        HttpRange range = HttpRange.parseFirst("bytes 5777705-27577450/27577451");
        Assert.assertEquals(new HttpRange(5777705, 27577450, 27577451), range);
    }

    @Test
    public void testFirstMultipleNormalRanges() {
        HttpRange range = HttpRange.parseFirst("bytes=100-1000,2000-2100");
        Assert.assertEquals(new HttpRange(100, 1000), range);
    }

    @Test
    public void testNormalRange() {
        HttpRange[] ranges = HttpRange.parse("bytes 100-1000");
        Assert.assertNotNull(ranges);
        Assert.assertEquals(1, ranges.length);
        Assert.assertEquals(new HttpRange(100, 1000), ranges[0]);
    }

    @Test
    public void testTwoNormalRanges() {
        HttpRange[] ranges = HttpRange.parse("bytes 100-1000,2000-2100");
        Assert.assertNotNull(ranges);
        Assert.assertEquals(2, ranges.length);
        Assert.assertEquals(new HttpRange(100, 1000), ranges[0]);
        Assert.assertEquals(new HttpRange(2000, 2100), ranges[1]);
    }

    @Test
    public void testMultipleNormalRanges() {
        HttpRange[] ranges = HttpRange.parse("bytes 100-1000,2000-2100,3000-3100,4000-4100");
        Assert.assertNotNull(ranges);
        Assert.assertEquals(4, ranges.length);
        Assert.assertEquals(new HttpRange(100, 1000), ranges[0]);
        Assert.assertEquals(new HttpRange(2000, 2100), ranges[1]);
        Assert.assertEquals(new HttpRange(3000, 3100), ranges[2]);
        Assert.assertEquals(new HttpRange(4000, 4100), ranges[3]);
    }

    @Test
    public void testFirstPartialRange() {
        HttpRange range = HttpRange.parseFirst("bytes 100-");
        Assert.assertEquals(new HttpRange(100, 0), range);
    }

    @Test
    public void testBoundingMultipleNormalRanges() {
        HttpRange range = HttpRange.parseBounding("bytes 100-1000,2000-2100,3000-3100,4000-4100");
        Assert.assertEquals(new HttpRange(100, 4100), range);
    }

    @Test
    public void testBoundingMultiplePartialRanges() {
        HttpRange range = HttpRange.parseBounding("bytes 100-1000,2000-,10-3100,4000-4100");
        Assert.assertEquals(new HttpRange(10, 0), range);
    }
}
