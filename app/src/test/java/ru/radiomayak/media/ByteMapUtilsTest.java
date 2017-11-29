package ru.radiomayak.media;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class ByteMapUtilsTest {
    private File file;

    @Before
    public void before() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        String name = UUID.randomUUID().toString();

        file = new File(tempDir, name);
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(file);
    }

    @Test
    public void shouldCreateNonexistentFile() throws IOException {
        ByteMapUtils.updateFile(file, 0, 10, 100, toOutputStream(91));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(randomAccessFile);
            Assert.assertNotNull(byteMap);
            Assert.assertEquals(0, byteMap.capacity());
            Assert.assertEquals(91, byteMap.size());
            byte[] data = new byte[91];
            int n = randomAccessFile.read(data);
            Assert.assertEquals(data.length, n);
            assertMonotonous(data, 0, data.length, 0);
        }
    }

    @Test
    public void shouldAppendNonIntersectingData() throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = new ByteMap(0, new int[] {0, 10});
            ByteMapUtils.writeHeader(randomAccessFile, byteMap);
            randomAccessFile.write(toOutputStream(11).toByteArray());
        }
        ByteMapUtils.updateFile(file, 0, 20, 100, toOutputStream(81));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(randomAccessFile);
            Assert.assertNotNull(byteMap);
            Assert.assertEquals(0, byteMap.capacity());
            Assert.assertEquals(92, byteMap.size());
            byte[] data = new byte[92];
            int n = randomAccessFile.read(data);
            Assert.assertEquals(data.length, n);
            assertMonotonous(data, 0, 11, 0);
            assertMonotonous(data, 11, data.length - 11, 0);
        }
    }

    @Test
    public void shouldAppendComplementData() throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = new ByteMap(0, new int[] {0, 10});
            ByteMapUtils.writeHeader(randomAccessFile, byteMap);
            randomAccessFile.write(toOutputStream(11).toByteArray());
        }
        ByteMapUtils.updateFile(file, 0, 11, 100, toOutputStream(90));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(randomAccessFile);
            Assert.assertNotNull(byteMap);
            Assert.assertEquals(0, byteMap.capacity());
            Assert.assertEquals(101, byteMap.size());
            byte[] data = new byte[101];
            int n = randomAccessFile.read(data);
            Assert.assertEquals(data.length, n);
            assertMonotonous(data, 0, 11, 0);
            assertMonotonous(data, 11, data.length - 11, 0);
        }
    }

    @Test
    public void shouldAppendIntersectingData() throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = new ByteMap(1000, new int[] {0, 50});
            ByteMapUtils.writeHeader(randomAccessFile, byteMap);
            randomAccessFile.write(toOutputStream(51).toByteArray());
        }
        ByteMapUtils.updateFile(file, 0, 21, 100, toOutputStream(80));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(randomAccessFile);
            Assert.assertNotNull(byteMap);
            Assert.assertEquals(1000, byteMap.capacity());
            Assert.assertEquals(101, byteMap.size());
            byte[] data = new byte[101];
            int n = randomAccessFile.read(data);
            Assert.assertEquals(data.length, n);
            assertMonotonous(data, 0, 21, 0);
            assertMonotonous(data, 21, data.length - 21, 0);
        }
    }

    @Test
    public void shouldAppendMultipleIntersectingData() throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = new ByteMap(0, new int[] {0, 20, 50, 100});
            ByteMapUtils.writeHeader(randomAccessFile, byteMap);
            randomAccessFile.write(toOutputStream(72).toByteArray());
        }
        ByteMapUtils.updateFile(file, 1000, 11, 60, toOutputStream(50));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(randomAccessFile);
            Assert.assertNotNull(byteMap);
            Assert.assertEquals(1000, byteMap.capacity());
            Assert.assertEquals(101, byteMap.size());
            byte[] data = new byte[101];
            int n = randomAccessFile.read(data);
            Assert.assertEquals(data.length, n);
            assertMonotonous(data, 0, 11, 0);
            assertMonotonous(data, 11, 50, 0);
            assertMonotonous(data, 61, data.length - 61, 32);
        }
    }

    private static ByteArrayOutputStream toOutputStream(int length) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        for (int i = 0; i < length; i++) {
            outputStream.write(i);
        }
        return outputStream;
    }

    private static void assertMonotonous(byte[] bytes, int offset, int length, int start) {
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(bytes[i + offset], (byte)(start + i));
        }
    }
}
