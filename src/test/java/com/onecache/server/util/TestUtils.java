// Copyright (c) Onecache, Inc.
// SPDX-License-Identifier: MPL-2.0

package com.onecache.server.util;

import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.mockito.Mockito;

import com.onecache.core.Builder;
import com.onecache.core.Cache;
import com.onecache.core.index.CompactBaseWithExpireIndexFormat;
import com.onecache.core.io.Segment;
import com.onecache.core.util.CacheConfig;
import com.onecache.core.util.UnsafeAccess;

/**
 * Utility methods for unit tests
 *
 */
public class TestUtils {
  
  
  public static Cache createCache(long maxSize, long segmentSize, boolean offheap, boolean withExpireSupport) throws IOException {
    
    Builder b = new Builder("cache");
    b.withCacheMaximumSize(maxSize)
    .withCacheDataSegmentSize(segmentSize)
    .withTLSSupported(true);
    if (withExpireSupport) {
      b.withMainQueueIndexFormat(CompactBaseWithExpireIndexFormat.class.getName());
    }
    if (offheap) {
      return b.buildMemoryCache();
    } else {
      return b.buildDiskCache();
    }
  }
  
  
  /**
   * Creates new byte array and fill it with random data
   * @param size size of an array
   * @return array
   */
  public static byte[] randomBytes(int size) {
    byte[] bytes = new byte[size];
    Random r = new Random();
    r.nextBytes(bytes);
    return bytes;
  }
  
  public static String randomString(int size) {
    return com.onecache.core.util.Utils.getRandomStr(new Random(), size);
  }
  
  /**
   * Creates new byte array and fill it with random data
   * @param size size of an array
   * @return array
   */
  public static byte[] randomBytes(int size, Random r) {
    byte[] bytes = new byte[size];
    r.nextBytes(bytes);
    return bytes;
  }
  /**
   * Copies an array
   * @param arr array of bytes
   * @return copy
   */
  public static byte[] copy (byte[] arr) {
    byte[] buf = new byte[arr.length];
    System.arraycopy(arr, 0, buf, 0, buf.length);
    return buf;
  }
  
  /**
   * Allocates memory and fills it with random data
   * @param size memory size
   * @return pointer
   */
  public static long randomMemory(int size) {
    byte[] bytes = randomBytes(size);
    long ptr = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(bytes, 0, ptr, size);
    return ptr;
  }
  
  /**
   * Allocates memory and fills it with random data
   * @param size memory size
   * @return pointer
   */
  public static long randomMemory(int size, Random r) {
    byte[] bytes = randomBytes(size, r);
    long ptr = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(bytes, 0, ptr, size);
    return ptr;
  }
  
  /**
   * Creates copy of a memory buffer
   * @param ptr memory buffer
   * @param size size of a memory buffer
   * @return copy of a memory buffer (pointer)
   */
  public static long copyMemory(long ptr, int size) {
    long mem = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(ptr, mem, size);
    return mem;
  }
  
  public static long copyToMemory(byte[] arr) {
    long mem = UnsafeAccess.malloc(arr.length);
    UnsafeAccess.copy(arr,  0,  mem, arr.length);
    return mem;
  }
  
  public static long copyToMemory(String s) {
    byte[] arr = s.getBytes();
    long mem = UnsafeAccess.malloc(arr.length);
    UnsafeAccess.copy(arr,  0,  mem, arr.length);
    return mem;
  }
  
  public static DataOutputStream getOutputStreamForTest() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    return new DataOutputStream(baos);
  }
  
  public static RandomAccessFile saveToFile(Segment s) throws IOException {
    File f = File.createTempFile("segment", null);
    f.deleteOnExit();
    RandomAccessFile raf = new RandomAccessFile(f, "rw");
    s.save(raf);
    return raf;
  }
  
  public static CacheConfig mockConfigForTests(long segmentSize, long maxCacheSize) throws IOException {
    CacheConfig mock = Mockito.mock(CacheConfig.class, CALLS_REAL_METHODS);
    mock.init();
    // define segment size
    Mockito.when(mock.getCacheSegmentSize(Mockito.anyString())).thenReturn(segmentSize);
    // define maximum cache size
    Mockito.when(mock.getCacheMaximumSize(Mockito.anyString())).thenReturn(maxCacheSize);
    // data directory
    Path path = Files.createTempDirectory(null);
    File  dir = path.toFile();
    dir.deleteOnExit();
    Mockito.when(mock.getDataDir(Mockito.anyString())).thenReturn(dir.getAbsolutePath());
    return mock;
  }
  
  public static CacheConfig mockConfigForTests(long segmentSize, long maxCacheSize, String dataDir) throws IOException {
    CacheConfig mock = Mockito.mock(CacheConfig.class, CALLS_REAL_METHODS);
    mock.init();
    // define segment size
    Mockito.when(mock.getCacheSegmentSize(Mockito.anyString())).thenReturn(segmentSize);
    // define maximum cache size
    Mockito.when(mock.getCacheMaximumSize(Mockito.anyString())).thenReturn(maxCacheSize);
    Mockito.when(mock.getDataDir(Mockito.anyString())).thenReturn(dataDir);
    return mock;
  }
  
  
  public static void deleteDir(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Stream<Path> stream = Files.list(dir);
    stream.forEach( x -> {
      try {
        Files.delete(x);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });
    Files.delete(dir);
    if(Files.exists(dir)) {
      System.err.printf("Could not delete dir=%s\n", dir.toString());
    } else {
      System.out.printf("Deleted dir=%s\n", dir.toString());
    }
    stream.close();
  }
  
  public static void deleteCacheFiles(Cache cache) throws IOException {
    String snapshotDir = cache.getCacheConfig().getSnapshotDir(cache.getName());
    Path p = Paths.get(snapshotDir);
    deleteDir(p);
    String dataDir = cache.getCacheConfig().getDataDir(cache.getName());
    p = Paths.get(dataDir);
    deleteDir(p);
  }
  
  public static List<byte[]> loadGithubDataAsBytes() throws URISyntaxException, IOException{
    
    File dir = new File("./src/test/resources/github");
    File[] list = dir.listFiles();
    ArrayList<byte[]> dataList = new ArrayList<byte[]>();
    for (File ff: list) {
      String s = Files.readString(Paths.get(ff.toURI()));
      dataList.add(s.getBytes());
    }
    return dataList;
  }
  
  public static List<Long> loadGithubDataAsMemory() throws URISyntaxException, IOException{
    
    File dir = new File("./src/test/resources/github");
    File[] list = dir.listFiles();
    ArrayList<Long> dataList = new ArrayList<Long>();
    for (File ff: list) {
      String s = Files.readString(Paths.get(ff.toURI()));
      long ptr = copyToMemory(s);
      dataList.add(ptr);
    }
    return dataList;
  }
    
}